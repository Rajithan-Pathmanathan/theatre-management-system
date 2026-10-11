package com.vijaya.theatre.booking;

import com.vijaya.theatre.AbstractIntegrationTest;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UC-08 functional correctness. The concurrency guarantee itself (FR-12,
 * NFR-02) is covered separately in BookingConcurrencyIntegrationTest, which
 * can't use this class's @Transactional wrapping (it needs real concurrent
 * transactions, not one shared test transaction).
 */
class BookingControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ShowSeatAllocationRepository showSeatAllocationRepository;

    @Test
    void bookingTwoSeats_locksThemAndReturnsAPendingBookingWithTheCorrectTotal() throws Exception {
        String token = loginAsNewAdmin();
        UUID showId = createShow(token, new BigDecimal("450.00"), new BigDecimal("300.00"));
        List<UUID> seatIds = onlineSeatIds(showId, 2);

        String body = """
                {"showId":"%s","adultCount":1,"childCount":1,"seatAllocationIds":["%s","%s"],
                 "customerName":"Nimal Silva","customerPhone":"+94771234567","customerEmail":null}
                """.formatted(showId, seatIds.get(0), seatIds.get(1));

        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING_PAYMENT")))
                .andExpect(jsonPath("$.totalAmount", is(750.00)))
                .andExpect(jsonPath("$.seats.length()", is(2)))
                .andExpect(jsonPath("$.bookingReference").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());

        for (UUID seatId : seatIds) {
            ShowSeatAllocation allocation = showSeatAllocationRepository.findById(seatId).orElseThrow();
            assertThat(allocation.getStatus()).isEqualTo(AllocationStatus.LOCKED);
            assertThat(allocation.getLockedUntil()).isAfter(Instant.now());
        }
    }

    @Test
    void seatCountNotMatchingTicketCount_violatesBR04() throws Exception {
        String token = loginAsNewAdmin();
        UUID showId = createShow(token, new BigDecimal("450.00"), new BigDecimal("300.00"));
        List<UUID> seatIds = onlineSeatIds(showId, 1); // only one seat, but asking for 2 tickets

        String body = """
                {"showId":"%s","adultCount":1,"childCount":1,"seatAllocationIds":["%s"],
                 "customerName":"Test","customerPhone":"+94771234567","customerEmail":null}
                """.formatted(showId, seatIds.get(0));

        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void duplicateSeatIdInRequest_isRejected() throws Exception {
        String token = loginAsNewAdmin();
        UUID showId = createShow(token, new BigDecimal("450.00"), new BigDecimal("300.00"));
        UUID seatId = onlineSeatIds(showId, 1).get(0);

        String body = """
                {"showId":"%s","adultCount":2,"childCount":0,"seatAllocationIds":["%s","%s"],
                 "customerName":"Test","customerPhone":"+94771234567","customerEmail":null}
                """.formatted(showId, seatId, seatId);

        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void bookingAnAlreadyBookedSeat_isRejected() throws Exception {
        String token = loginAsNewAdmin();
        UUID showId = createShow(token, new BigDecimal("450.00"), new BigDecimal("300.00"));
        UUID seatId = onlineSeatIds(showId, 1).get(0);

        ShowSeatAllocation allocation = showSeatAllocationRepository.findById(seatId).orElseThrow();
        allocation.setStatus(AllocationStatus.BOOKED);
        showSeatAllocationRepository.save(allocation);

        String body = """
                {"showId":"%s","adultCount":1,"childCount":0,"seatAllocationIds":["%s"],
                 "customerName":"Test","customerPhone":"+94771234567","customerEmail":null}
                """.formatted(showId, seatId);

        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void aSeatWhoseLockHasExpired_canBeBookedAgain() throws Exception {
        String token = loginAsNewAdmin();
        UUID showId = createShow(token, new BigDecimal("450.00"), new BigDecimal("300.00"));
        UUID seatId = onlineSeatIds(showId, 1).get(0);

        ShowSeatAllocation allocation = showSeatAllocationRepository.findById(seatId).orElseThrow();
        allocation.setStatus(AllocationStatus.LOCKED);
        allocation.setLockedUntil(Instant.now().minusSeconds(60)); // expired a minute ago
        allocation.setLockToken(UUID.randomUUID());
        showSeatAllocationRepository.save(allocation);

        String body = """
                {"showId":"%s","adultCount":1,"childCount":0,"seatAllocationIds":["%s"],
                 "customerName":"Test","customerPhone":"+94771234567","customerEmail":null}
                """.formatted(showId, seatId);

        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void bookingAPhysicalChannelSeat_isRejected_evenWhenTheShowItselfIsOnlineBookable() throws Exception {
        String token = loginAsNewAdmin();
        // Online booking IS enabled for the show — just targeting one of the many seats that
        // landed in the PHYSICAL channel at 40% allocation, to isolate the per-seat channel check.
        UUID showId = createShow(token, new BigDecimal("450.00"), new BigDecimal("300.00"), 40);
        UUID physicalSeatId = showSeatAllocationRepository.findByShowId(showId).stream()
                .filter(a -> a.getChannel() == AllocationChannel.PHYSICAL).findFirst().orElseThrow().getId();

        String body = """
                {"showId":"%s","adultCount":1,"childCount":0,"seatAllocationIds":["%s"],
                 "customerName":"Test","customerPhone":"+94771234567","customerEmail":null}
                """.formatted(showId, physicalSeatId);

        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void bookingAShowWithOnlineBookingDisabled_isRejected() throws Exception {
        String token = loginAsNewAdmin();
        // guaranteed=true but onlineBookingEnabled=false is a valid combination at show-creation
        // time (UC-05) — this proves BookingService checks it again independently at booking time.
        UUID showId = createShowWithOnlineBookingDisabled(token);
        UUID seatId = onlineSeatIds(showId, 1).stream().findFirst()
                .orElseGet(() -> showSeatAllocationRepository.findByShowId(showId).get(0).getId());

        String body = """
                {"showId":"%s","adultCount":1,"childCount":0,"seatAllocationIds":["%s"],
                 "customerName":"Test","customerPhone":"+94771234567","customerEmail":null}
                """.formatted(showId, seatId);

        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    private UUID createShowWithOnlineBookingDisabled(String adminToken) throws Exception {
        MovieRequest movieRequest = new MovieRequest("Disabled Online Test " + UUID.randomUUID(), "Tamil", "Drama", 140, null, null);
        String movieResponse = mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movieRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID movieId = UUID.fromString(objectMapper.readTree(movieResponse).get("id").asString());

        ShowRequest showRequest = new ShowRequest(movieId, LocalDate.now().plusDays(21), LocalTime.of(18, 30),
                new BigDecimal("450.00"), new BigDecimal("300.00"), true, false, 0, true);
        String showResponse = mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(showResponse).get("id").asString());
    }

    private UUID createShow(String adminToken, BigDecimal adultPrice, BigDecimal childPrice) throws Exception {
        return createShow(adminToken, adultPrice, childPrice, 100);
    }

    private UUID createShow(String adminToken, BigDecimal adultPrice, BigDecimal childPrice, int onlineAllocationPercent) throws Exception {
        MovieRequest movieRequest = new MovieRequest("Booking Test Movie " + UUID.randomUUID(), "Tamil", "Drama", 140, null, null);
        String movieResponse = mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movieRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID movieId = UUID.fromString(objectMapper.readTree(movieResponse).get("id").asString());

        ShowRequest showRequest = new ShowRequest(movieId, LocalDate.now().plusDays(20), LocalTime.of(18, 30),
                adultPrice, childPrice, true, onlineAllocationPercent > 0, onlineAllocationPercent, true);
        String showResponse = mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(showResponse).get("id").asString());
    }

    private List<UUID> onlineSeatIds(UUID showId, int count) {
        return showSeatAllocationRepository.findByShowId(showId).stream()
                .filter(a -> a.getChannel() == AllocationChannel.ONLINE)
                .limit(count)
                .map(ShowSeatAllocation::getId)
                .toList();
    }
}
