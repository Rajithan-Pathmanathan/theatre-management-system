package com.vijaya.theatre.canteen;

import com.vijaya.theatre.AbstractIntegrationTest;
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
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** BR-13 — snacks chosen on a booking's payment page are charged with the tickets. Signs PayHere callbacks like PaymentControllerIntegrationTest. */
class BookingSnackPaymentIntegrationTest extends AbstractIntegrationTest {

    private static final String MERCHANT_ID = "sandbox-merchant-id";
    private static final String MERCHANT_SECRET = "sandbox-merchant-secret";

    @Autowired
    private ShowSeatAllocationRepository showSeatAllocationRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private SnackOrderRepository snackOrderRepository;

    @Test
    void snacksAreChargedWithTheTicketsAndJoinTheQueueOncePaid() throws Exception {
        String adminToken = loginAsNewAdmin();
        UUID bookingId = createBooking(createShow(adminToken)); // tickets: 450 + 300 = 750
        String popcornId = createSnackItem(adminToken, "800.00");

        String orderId = objectMapper.readTree(setSnacks(bookingId, popcornId, 1)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status", is("AWAITING_PAYMENT")))
                        .andReturn().getResponse().getContentAsString())
                .get("id").asString();

        // Not in the canteen's hands until the booking is paid.
        mockMvc.perform(patch("/api/canteen/orders/" + orderId + "/status").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"READY\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/bookings/" + bookingId + "/payment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount", is("1550.00")));

        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        sendNotify(booking.getBookingReference(), "1550.00", "2").andExpect(status().isOk());

        SnackOrder paid = snackOrderRepository.findById(UUID.fromString(orderId)).orElseThrow();
        assertThat(paid.getStatus()).isEqualTo(SnackOrderStatus.PLACED);
        assertThat(paid.getPrepaidAmount()).isEqualByComparingTo("800.00");
        // Ticket reporting reads booking.totalAmount, so it must stay tickets-only.
        assertThat(bookingRepository.findById(bookingId).orElseThrow().getTotalAmount()).isEqualByComparingTo("750.00");

        // Too late to change once paid.
        setSnacks(bookingId, popcornId, 2).andExpect(status().isConflict());
    }

    @Test
    void savingTheSnacksAgainReplacesThem_soNothingIsChargedTwice() throws Exception {
        String adminToken = loginAsNewAdmin();
        UUID bookingId = createBooking(createShow(adminToken));
        String popcornId = createSnackItem(adminToken, "800.00");

        setSnacks(bookingId, popcornId, 1).andExpect(status().isOk());
        setSnacks(bookingId, popcornId, 2).andExpect(status().isOk());
        mockMvc.perform(post("/api/bookings/" + bookingId + "/payment"))
                .andExpect(jsonPath("$.amount", is("2350.00"))); // 750 + 2 × 800, not 3 × 800

        mockMvc.perform(put("/api/snack-orders/booking/" + bookingId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickupTiming\":\"BEFORE_MOVIE\",\"items\":[]}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/bookings/" + bookingId + "/payment"))
                .andExpect(jsonPath("$.amount", is("750.00")));
        assertThat(snackOrderRepository.findByLinkedBookingIdAndStatus(bookingId, SnackOrderStatus.AWAITING_PAYMENT)).isEmpty();
    }

    private ResultActions setSnacks(UUID bookingId, String snackItemId, int quantity) throws Exception {
        return mockMvc.perform(put("/api/snack-orders/booking/" + bookingId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"pickupTiming\":\"BEFORE_MOVIE\",\"items\":[{\"snackItemId\":\"" + snackItemId + "\",\"quantity\":" + quantity + "}]}"));
    }

    private ResultActions sendNotify(String orderId, String amount, String statusCode) throws Exception {
        String hashedSecret = md5Hex(MERCHANT_SECRET).toUpperCase();
        String signature = md5Hex(MERCHANT_ID + orderId + amount + "LKR" + statusCode + hashedSecret).toUpperCase();
        return mockMvc.perform(post("/api/payments/payhere/notify")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("merchant_id", MERCHANT_ID)
                .param("order_id", orderId)
                .param("payment_id", "PH-BR13")
                .param("payhere_amount", amount)
                .param("payhere_currency", "LKR")
                .param("status_code", statusCode)
                .param("md5sig", signature));
    }

    private String md5Hex(String input) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(input.getBytes(StandardCharsets.UTF_8)));
    }

    private String createSnackItem(String adminToken, String price) throws Exception {
        String response = mockMvc.perform(post("/api/admin/snack-items").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"BR-13 Popcorn " + UUID.randomUUID() + "\",\"price\":" + price + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asString();
    }

    private UUID createBooking(UUID showId) throws Exception {
        List<UUID> seatIds = showSeatAllocationRepository.findByShowId(showId).stream()
                .filter(a -> a.getChannel() == AllocationChannel.ONLINE)
                .limit(2)
                .map(ShowSeatAllocation::getId)
                .toList();
        String body = """
                {"showId":"%s","adultCount":1,"childCount":1,"seatAllocationIds":["%s","%s"],
                 "customerName":"Snack Booker","customerPhone":"+94771230000","customerEmail":null}
                """.formatted(showId, seatIds.get(0), seatIds.get(1));
        String response = mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asString());
    }

    private UUID createShow(String adminToken) throws Exception {
        MovieRequest movieRequest = new MovieRequest("BR-13 Test Movie " + UUID.randomUUID(), "Tamil", "Drama", 140, null, null);
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
                new BigDecimal("450.00"), new BigDecimal("300.00"), true, true, 100, true);
        String showResponse = mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(showResponse).get("id").asString());
    }
}
