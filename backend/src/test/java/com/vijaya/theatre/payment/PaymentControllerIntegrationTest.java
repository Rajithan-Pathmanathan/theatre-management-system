package com.vijaya.theatre.payment;

import com.vijaya.theatre.AbstractIntegrationTest;
import com.vijaya.theatre.booking.Booking;
import com.vijaya.theatre.booking.BookingRepository;
import com.vijaya.theatre.booking.BookingStatus;
import com.vijaya.theatre.movie.dto.MovieRequest;
import com.vijaya.theatre.show.AllocationChannel;
import com.vijaya.theatre.show.AllocationStatus;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UC-09. These tests act as PayHere itself would: they sign the notify
 * request using the same sandbox merchant-secret the running app was
 * configured with (see application.yml's app.payhere.* defaults), the same
 * way the real gateway would sign a callback with the real merchant secret.
 */
class PaymentControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String MERCHANT_ID = "sandbox-merchant-id";
    private static final String MERCHANT_SECRET = "sandbox-merchant-secret";

    @Autowired
    private ShowSeatAllocationRepository showSeatAllocationRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Test
    void initiatePayment_returnsASignedCheckoutPayloadMatchingTheBookingTotal() throws Exception {
        String token = loginAsNewAdmin();
        UUID showId = createShow(token);
        UUID bookingId = createBooking(showId);

        mockMvc.perform(post("/api/bookings/" + bookingId + "/payment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantId", is(MERCHANT_ID)))
                .andExpect(jsonPath("$.amount", is("750.00")))
                .andExpect(jsonPath("$.currency", is("LKR")))
                .andExpect(jsonPath("$.hash").isNotEmpty());

        PaymentTransaction transaction = paymentTransactionRepository.findByBookingId(bookingId).orElseThrow();
        assertThat(transaction.getStatus()).isEqualTo(PaymentStatus.INITIATED);
        assertThat(transaction.getGateway()).isEqualTo(PaymentGateway.PAYHERE);
    }

    @Test
    void notifyWithSuccessStatus_confirmsTheBookingAndBooksTheSeatsAndIssuesAQrToken() throws Exception {
        String token = loginAsNewAdmin();
        UUID showId = createShow(token);
        UUID bookingId = createBooking(showId);
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();

        mockMvc.perform(post("/api/bookings/" + bookingId + "/payment")).andExpect(status().isOk());

        sendNotify(booking.getBookingReference(), "750.00", "2").andExpect(status().isOk());

        Booking confirmed = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(confirmed.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(confirmed.getConfirmedAt()).isNotNull();
        assertThat(confirmed.getQrToken()).isNotNull();
        assertThat(confirmed.getExpiresAt()).isNull();

        List<ShowSeatAllocation> allocations = showSeatAllocationRepository.findByShowId(showId).stream()
                .filter(a -> a.getStatus() == AllocationStatus.BOOKED)
                .toList();
        assertThat(allocations).hasSize(2);
        allocations.forEach(a -> {
            assertThat(a.getLockedUntil()).isNull();
            assertThat(a.getLockToken()).isNull();
        });

        PaymentTransaction transaction = paymentTransactionRepository.findByBookingId(bookingId).orElseThrow();
        assertThat(transaction.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(transaction.getCompletedAt()).isNotNull();
    }

    @Test
    void notifyWithAnInvalidSignature_isRejectedAndTheBookingStaysPending() throws Exception {
        String token = loginAsNewAdmin();
        UUID showId = createShow(token);
        UUID bookingId = createBooking(showId);
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        mockMvc.perform(post("/api/bookings/" + bookingId + "/payment")).andExpect(status().isOk());

        mockMvc.perform(post("/api/payments/payhere/notify")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("merchant_id", MERCHANT_ID)
                        .param("order_id", booking.getBookingReference())
                        .param("payment_id", "PH12345")
                        .param("payhere_amount", "750.00")
                        .param("payhere_currency", "LKR")
                        .param("status_code", "2")
                        .param("md5sig", "not-a-real-signature"))
                .andExpect(status().isConflict());

        assertThat(bookingRepository.findById(bookingId).orElseThrow().getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void notifyWithAFailureStatus_marksTheTransactionFailedButLeavesTheBookingPending() throws Exception {
        String token = loginAsNewAdmin();
        UUID showId = createShow(token);
        UUID bookingId = createBooking(showId);
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        mockMvc.perform(post("/api/bookings/" + bookingId + "/payment")).andExpect(status().isOk());

        sendNotify(booking.getBookingReference(), "750.00", "-2").andExpect(status().isOk()); // -2 = failed

        assertThat(bookingRepository.findById(bookingId).orElseThrow().getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(paymentTransactionRepository.findByBookingId(bookingId).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void aDuplicateNotification_isIgnoredIdempotentlyInsteadOfReprocessed() throws Exception {
        String token = loginAsNewAdmin();
        UUID showId = createShow(token);
        UUID bookingId = createBooking(showId);
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        mockMvc.perform(post("/api/bookings/" + bookingId + "/payment")).andExpect(status().isOk());

        sendNotify(booking.getBookingReference(), "750.00", "2").andExpect(status().isOk());
        UUID firstQrToken = bookingRepository.findById(bookingId).orElseThrow().getQrToken();

        sendNotify(booking.getBookingReference(), "750.00", "2").andExpect(status().isOk());
        assertThat(bookingRepository.findById(bookingId).orElseThrow().getQrToken()).isEqualTo(firstQrToken);
    }

    @Test
    void qrCode_isAvailableOnceConfirmed_butRejectedWhileStillPending() throws Exception {
        String token = loginAsNewAdmin();
        UUID showId = createShow(token);
        UUID bookingId = createBooking(showId);
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        mockMvc.perform(post("/api/bookings/" + bookingId + "/payment")).andExpect(status().isOk());

        mockMvc.perform(get("/api/bookings/" + bookingId + "/qr-code")).andExpect(status().isConflict());

        sendNotify(booking.getBookingReference(), "750.00", "2").andExpect(status().isOk());

        mockMvc.perform(get("/api/bookings/" + bookingId + "/qr-code"))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentType()).isEqualTo(MediaType.IMAGE_PNG_VALUE))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray().length).isGreaterThan(0));
    }

    private org.springframework.test.web.servlet.ResultActions sendNotify(String orderId, String amount, String statusCode) throws Exception {
        String signature = sign(orderId, amount, statusCode);
        return mockMvc.perform(post("/api/payments/payhere/notify")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("merchant_id", MERCHANT_ID)
                .param("order_id", orderId)
                .param("payment_id", "PH12345")
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

    private UUID createBooking(UUID showId) throws Exception {
        List<UUID> seatIds = showSeatAllocationRepository.findByShowId(showId).stream()
                .filter(a -> a.getChannel() == AllocationChannel.ONLINE)
                .limit(2)
                .map(ShowSeatAllocation::getId)
                .toList();

        String body = """
                {"showId":"%s","adultCount":1,"childCount":1,"seatAllocationIds":["%s","%s"],
                 "customerName":"Kamal Perera","customerPhone":"+94771234567","customerEmail":null}
                """.formatted(showId, seatIds.get(0), seatIds.get(1));

        String response = mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asString());
    }

    private UUID createShow(String adminToken) throws Exception {
        MovieRequest movieRequest = new MovieRequest("Payment Test Movie " + UUID.randomUUID(), "Tamil", "Drama", 140, null, null);
        String movieResponse = mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movieRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID movieId = UUID.fromString(objectMapper.readTree(movieResponse).get("id").asString());

        ShowRequest showRequest = new ShowRequest(movieId, LocalDate.now().plusDays(22), LocalTime.of(18, 30),
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
