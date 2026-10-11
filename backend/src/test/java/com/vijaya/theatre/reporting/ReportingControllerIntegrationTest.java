package com.vijaya.theatre.reporting;

import com.vijaya.theatre.AbstractIntegrationTest;
import com.vijaya.theatre.auth.StaffRole;
import com.vijaya.theatre.movie.dto.MovieRequest;
import com.vijaya.theatre.show.AllocationChannel;
import com.vijaya.theatre.show.ShowSeatAllocation;
import com.vijaya.theatre.show.ShowSeatAllocationRepository;
import com.vijaya.theatre.show.dto.ShowRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC-14, FR-33–FR-39. */
class ReportingControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String MERCHANT_ID = "sandbox-merchant-id";
    private static final String MERCHANT_SECRET = "sandbox-merchant-secret";

    @Autowired
    private ShowSeatAllocationRepository showSeatAllocationRepository;

    @Test
    void showReport_onlyCountsConfirmedCounterSales() throws Exception {
        String adminToken = loginAsNewAdmin();
        UUID showId = createShow(adminToken);
        sellCounterTicket(adminToken, showId, "+94772220001");
        sellCounterTicket(adminToken, showId, "+94772220002");

        mockMvc.perform(get("/api/admin/reports/shows/" + showId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketsSold", is(2)))
                .andExpect(jsonPath("$.collections", is(900.00)))
                .andExpect(jsonPath("$.physical.ticketsSold", is(2)))
                .andExpect(jsonPath("$.physical.collections", is(900.00)))
                .andExpect(jsonPath("$.online.ticketsSold", is(0)));
    }

    @Test
    void dailyReport_splitsOnlineAndPhysicalAndExcludesPendingPayment() throws Exception {
        String adminToken = loginAsNewAdmin();
        String showResponse = createShowRaw(adminToken);
        UUID showId = UUID.fromString(objectMapper.readTree(showResponse).get("id").asString());
        LocalDate showDate = LocalDate.parse(objectMapper.readTree(showResponse).get("showDate").asString());

        // one CONFIRMED counter sale (physical)
        sellCounterTicket(adminToken, showId, "+94772220010");

        // one CONFIRMED online sale (via a real signed PayHere notify)
        List<UUID> onlineSeatIds = showSeatAllocationRepository.findByShowId(showId).stream()
                .filter(a -> a.getChannel() == AllocationChannel.ONLINE)
                .limit(1).map(ShowSeatAllocation::getId).toList();
        String bookingBody = """
                {"showId":"%s","adultCount":1,"childCount":0,"seatAllocationIds":["%s"],
                 "customerName":"Online Report Test","customerPhone":"+94772220011","customerEmail":null}
                """.formatted(showId, onlineSeatIds.get(0));
        String bookingResponse = mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(bookingBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String onlineRef = objectMapper.readTree(bookingResponse).get("bookingReference").asString();
        UUID onlineBookingId = UUID.fromString(objectMapper.readTree(bookingResponse).get("id").asString());
        mockMvc.perform(post("/api/bookings/" + onlineBookingId + "/payment")).andExpect(status().isOk());
        sendNotify(onlineRef, "450.00", "2").andExpect(status().isOk());

        // one PENDING_PAYMENT online booking that must NOT count
        List<UUID> secondOnlineSeatIds = showSeatAllocationRepository.findByShowId(showId).stream()
                .filter(a -> a.getChannel() == AllocationChannel.ONLINE)
                .skip(1).limit(1).map(ShowSeatAllocation::getId).toList();
        String pendingBody = """
                {"showId":"%s","adultCount":1,"childCount":0,"seatAllocationIds":["%s"],
                 "customerName":"Pending Report Test","customerPhone":"+94772220012","customerEmail":null}
                """.formatted(showId, secondOnlineSeatIds.get(0));
        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(pendingBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/reports/daily").param("date", showDate.toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTicketsSold", is(2)))
                .andExpect(jsonPath("$.online.ticketsSold", is(1)))
                .andExpect(jsonPath("$.online.collections", is(450.00)))
                .andExpect(jsonPath("$.physical.ticketsSold", is(1)))
                .andExpect(jsonPath("$.physical.collections", is(450.00)))
                .andExpect(jsonPath("$.shows.length()", is(1)))
                .andExpect(jsonPath("$.shows[0].showId", is(showId.toString())));
    }

    @Test
    void aShowWithNoConfirmedBookings_reportsZeroes() throws Exception {
        String adminToken = loginAsNewAdmin();
        UUID showId = createShow(adminToken);

        mockMvc.perform(get("/api/admin/reports/shows/" + showId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketsSold", is(0)))
                .andExpect(jsonPath("$.collections", is(0)));
    }

    @Test
    void nonAdminCannotAccessReports() throws Exception {
        String username = "counter-" + UUID.randomUUID().toString().substring(0, 8);
        seedStaff(username, "a-strong-password", StaffRole.COUNTER_STAFF);
        String counterToken = loginAs(username, "a-strong-password");

        mockMvc.perform(get("/api/admin/reports/daily").param("date", LocalDate.now().toString())
                        .header("Authorization", "Bearer " + counterToken))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.ResultActions sendNotify(String orderId, String amount, String statusCode) throws Exception {
        String signature = sign(orderId, amount, statusCode);
        return mockMvc.perform(post("/api/payments/payhere/notify")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("merchant_id", MERCHANT_ID)
                .param("order_id", orderId)
                .param("payment_id", "PH-REPORT-TEST")
                .param("payhere_amount", amount)
                .param("payhere_currency", "LKR")
                .param("status_code", statusCode)
                .param("md5sig", signature));
    }

    private String sign(String orderId, String amount, String statusCode) throws Exception {
        String hashedSecret = md5Hex(MERCHANT_SECRET).toUpperCase();
        return md5Hex(MERCHANT_ID + orderId + amount + "LKR" + statusCode + hashedSecret).toUpperCase();
    }

    private String md5Hex(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
    }

    private String sellCounterTicket(String adminToken, UUID showId, String phone) throws Exception {
        UUID seatId = showSeatAllocationRepository.findByShowId(showId).stream()
                .filter(a -> a.getChannel() == AllocationChannel.PHYSICAL)
                .map(ShowSeatAllocation::getId)
                .findFirst().orElseThrow();

        String body = """
                {"showId":"%s","adultCount":1,"childCount":0,"seatAllocationIds":["%s"],
                 "physicalPaymentMethod":"CASH","customerName":"Report Test Customer","customerPhone":"%s"}
                """.formatted(showId, seatId, phone);

        String response = mockMvc.perform(post("/api/counter/sales").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("bookingReference").asString();
    }

    private UUID createShow(String adminToken) throws Exception {
        return UUID.fromString(objectMapper.readTree(createShowRaw(adminToken)).get("id").asString());
    }

    private String createShowRaw(String adminToken) throws Exception {
        MovieRequest movieRequest = new MovieRequest("Reporting Test Movie " + UUID.randomUUID(), "Tamil", "Drama", 140, null, null);
        String movieResponse = mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movieRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID movieId = UUID.fromString(objectMapper.readTree(movieResponse).get("id").asString());

        int dayOffset = ThreadLocalRandom.current().nextInt(24, 300);
        int hour = ThreadLocalRandom.current().nextInt(10, 23);
        ShowRequest showRequest = new ShowRequest(movieId, LocalDate.now().plusDays(dayOffset), LocalTime.of(hour, 0),
                new BigDecimal("450.00"), new BigDecimal("300.00"), true, true, 40, true);
        return mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }
}
