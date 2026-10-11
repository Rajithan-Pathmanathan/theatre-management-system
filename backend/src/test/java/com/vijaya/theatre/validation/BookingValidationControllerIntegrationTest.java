package com.vijaya.theatre.validation;

import com.vijaya.theatre.AbstractIntegrationTest;
import com.vijaya.theatre.auth.StaffRole;
import com.vijaya.theatre.booking.Booking;
import com.vijaya.theatre.booking.BookingRepository;
import com.vijaya.theatre.movie.dto.MovieRequest;
import com.vijaya.theatre.show.AllocationChannel;
import com.vijaya.theatre.show.ShowSeatAllocation;
import com.vijaya.theatre.show.ShowSeatAllocationRepository;
import com.vijaya.theatre.show.dto.ShowRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC-11, FR-28–FR-32, BR-11, BR-12. */
class BookingValidationControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ShowSeatAllocationRepository showSeatAllocationRepository;
    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void lookupByReference_findsTheBookingWithItsRemainingTicketCount() throws Exception {
        String adminToken = loginAsNewAdmin();
        String counterToken = loginAsNewCounterOrDoorStaff();
        String bookingRef = sellCounterTicket(adminToken, counterToken, "+94779990001", 1, 1);

        mockMvc.perform(get("/api/validation/lookup").param("reference", bookingRef).header("Authorization", "Bearer " + counterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].bookingReference", is(bookingRef)))
                .andExpect(jsonPath("$[0].remainingTickets", is(2)))
                .andExpect(jsonPath("$[0].ticketsUsed", is(0)));
    }

    @Test
    void lookupByQrToken_findsTheSameBooking() throws Exception {
        String adminToken = loginAsNewAdmin();
        String counterToken = loginAsNewCounterOrDoorStaff();
        String bookingRef = sellCounterTicket(adminToken, counterToken, "+94779990002", 1, 0);
        Booking booking = bookingRepository.findByBookingReference(bookingRef).orElseThrow();

        mockMvc.perform(get("/api/validation/lookup").param("qrToken", booking.getQrToken().toString())
                        .header("Authorization", "Bearer " + counterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].bookingReference", is(bookingRef)));
    }

    @Test
    void lookupByPhone_canReturnSeveralBookings() throws Exception {
        String adminToken = loginAsNewAdmin();
        String counterToken = loginAsNewCounterOrDoorStaff();
        String phone = "+94779990099";
        sellCounterTicket(adminToken, counterToken, phone, 1, 0);
        sellCounterTicket(adminToken, counterToken, phone, 1, 0);

        mockMvc.perform(get("/api/validation/lookup").param("phone", phone).header("Authorization", "Bearer " + counterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)));
    }

    @Test
    void lookupWithoutExactlyOneParameter_isRejected() throws Exception {
        String counterToken = loginAsNewCounterOrDoorStaff();

        mockMvc.perform(get("/api/validation/lookup").header("Authorization", "Bearer " + counterToken))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/validation/lookup")
                        .param("reference", "VJY-20260101-AAAAAA")
                        .param("phone", "+94770000000")
                        .header("Authorization", "Bearer " + counterToken))
                .andExpect(status().isConflict());
    }

    @Test
    void partialCheckIn_admitsSomeTicketsThenTheRestLater() throws Exception {
        String adminToken = loginAsNewAdmin();
        String counterToken = loginAsNewCounterOrDoorStaff();
        String bookingRef = sellCounterTicket(adminToken, counterToken, "+94779990003", 1, 1);
        UUID bookingId = lookupBookingId(bookingRef, counterToken);

        mockMvc.perform(post("/api/validation/bookings/" + bookingId + "/check-in")
                        .header("Authorization", "Bearer " + counterToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ticketsToAdmit\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketsUsed", is(1)))
                .andExpect(jsonPath("$.remainingTickets", is(1)))
                .andExpect(jsonPath("$.fullyCheckedIn", is(false)));

        mockMvc.perform(post("/api/validation/bookings/" + bookingId + "/check-in")
                        .header("Authorization", "Bearer " + counterToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ticketsToAdmit\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketsUsed", is(2)))
                .andExpect(jsonPath("$.remainingTickets", is(0)))
                .andExpect(jsonPath("$.fullyCheckedIn", is(true)));
    }

    @Test
    void aFullyCheckedInBooking_cannotBeValidatedAgain() throws Exception {
        String adminToken = loginAsNewAdmin();
        String counterToken = loginAsNewCounterOrDoorStaff();
        String bookingRef = sellCounterTicket(adminToken, counterToken, "+94779990004", 1, 0);
        UUID bookingId = lookupBookingId(bookingRef, counterToken);

        mockMvc.perform(post("/api/validation/bookings/" + bookingId + "/check-in")
                        .header("Authorization", "Bearer " + counterToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ticketsToAdmit\":1}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/validation/bookings/" + bookingId + "/check-in")
                        .header("Authorization", "Bearer " + counterToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ticketsToAdmit\":1}"))
                .andExpect(status().isConflict());
    }

    @Test
    void admittingMoreTicketsThanRemain_isRejected() throws Exception {
        String adminToken = loginAsNewAdmin();
        String counterToken = loginAsNewCounterOrDoorStaff();
        String bookingRef = sellCounterTicket(adminToken, counterToken, "+94779990005", 1, 1);
        UUID bookingId = lookupBookingId(bookingRef, counterToken);

        mockMvc.perform(post("/api/validation/bookings/" + bookingId + "/check-in")
                        .header("Authorization", "Bearer " + counterToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ticketsToAdmit\":3}"))
                .andExpect(status().isConflict());
    }

    @Test
    void aPendingPaymentBooking_cannotBeValidated() throws Exception {
        String adminToken = loginAsNewAdmin();
        String counterToken = loginAsNewCounterOrDoorStaff();
        UUID showId = createShow(adminToken, 100);
        List<UUID> onlineSeatIds = showSeatAllocationRepository.findByShowId(showId).stream()
                .filter(a -> a.getChannel() == AllocationChannel.ONLINE)
                .limit(1)
                .map(ShowSeatAllocation::getId)
                .toList();

        String bookingBody = """
                {"showId":"%s","adultCount":1,"childCount":0,"seatAllocationIds":["%s"],
                 "customerName":"Pending Guy","customerPhone":"+94779990006","customerEmail":null}
                """.formatted(showId, onlineSeatIds.get(0));
        String bookingResponse = mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(bookingBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID bookingId = UUID.fromString(objectMapper.readTree(bookingResponse).get("id").asString());

        mockMvc.perform(post("/api/validation/bookings/" + bookingId + "/check-in")
                        .header("Authorization", "Bearer " + counterToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ticketsToAdmit\":1}"))
                .andExpect(status().isConflict());
    }

    private UUID lookupBookingId(String bookingRef, String token) throws Exception {
        String response = mockMvc.perform(get("/api/validation/lookup").param("reference", bookingRef)
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get(0).get("id").asString());
    }

    private String loginAsNewCounterOrDoorStaff() throws Exception {
        String username = "door-" + UUID.randomUUID().toString().substring(0, 8);
        seedStaff(username, "a-strong-password", StaffRole.COUNTER_STAFF);
        return loginAs(username, "a-strong-password");
    }

    private String sellCounterTicket(String adminToken, String counterToken, String phone, int adultCount, int childCount) throws Exception {
        UUID showId = createShow(adminToken, 40);
        int seatCount = adultCount + childCount;
        List<UUID> seatIds = showSeatAllocationRepository.findByShowId(showId).stream()
                .filter(a -> a.getChannel() == AllocationChannel.PHYSICAL)
                .limit(seatCount)
                .map(ShowSeatAllocation::getId)
                .toList();
        String seatIdsJson = seatIds.stream().map(id -> "\"" + id + "\"").reduce((a, b) -> a + "," + b).orElseThrow();

        String body = """
                {"showId":"%s","adultCount":%d,"childCount":%d,"seatAllocationIds":[%s],
                 "physicalPaymentMethod":"CASH","customerName":"Door Test Customer","customerPhone":"%s"}
                """.formatted(showId, adultCount, childCount, seatIdsJson, phone);

        String response = mockMvc.perform(post("/api/counter/sales").header("Authorization", "Bearer " + counterToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("bookingReference").asString();
    }

    private UUID createShow(String adminToken, int onlineAllocationPercent) throws Exception {
        MovieRequest movieRequest = new MovieRequest("Validation Test Movie " + UUID.randomUUID(), "Tamil", "Drama", 140, null, null);
        String movieResponse = mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movieRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID movieId = UUID.fromString(objectMapper.readTree(movieResponse).get("id").asString());

        // Random day/time per call (not a fixed slot) — this test class creates several shows per
        // test method, and the single-screen overlap rule (BR-01-adjacent) would otherwise collide them.
        int dayOffset = java.util.concurrent.ThreadLocalRandom.current().nextInt(24, 300);
        int hour = java.util.concurrent.ThreadLocalRandom.current().nextInt(10, 23);
        ShowRequest showRequest = new ShowRequest(movieId, LocalDate.now().plusDays(dayOffset), LocalTime.of(hour, 0),
                new BigDecimal("450.00"), new BigDecimal("300.00"), true, true, onlineAllocationPercent, true);
        String showResponse = mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(showResponse).get("id").asString());
    }
}
