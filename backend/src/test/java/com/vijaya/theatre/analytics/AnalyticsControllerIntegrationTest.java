package com.vijaya.theatre.analytics;

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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC-15, FR-40–FR-45. */
class AnalyticsControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ShowSeatAllocationRepository showSeatAllocationRepository;

    @Test
    void movieAnalytics_aggregatesTicketsAndOccupancyAcrossAllItsShows() throws Exception {
        String adminToken = loginAsNewAdmin();
        UUID movieId = createMovie(adminToken);
        UUID showId1 = createShow(adminToken, movieId);
        UUID showId2 = createShow(adminToken, movieId);

        String ref1 = sellCounterTicket(adminToken, showId1); // 1 ticket, not checked in
        String bookingId1 = bookingIdFor(adminToken, ref1);
        checkIn(adminToken, bookingId1, 1); // fully attended

        sellCounterTicket(adminToken, showId2); // 1 ticket, never checked in — drags attendance rate down

        mockMvc.perform(get("/api/admin/analytics/movies/" + movieId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalShows", is(2)))
                .andExpect(jsonPath("$.ticketsSold", is(2)))
                .andExpect(jsonPath("$.ticketsUsed", is(1)))
                .andExpect(jsonPath("$.attendanceRatePercent", is(50.0)))
                .andExpect(jsonPath("$.physical.ticketsSold", is(2)))
                .andExpect(jsonPath("$.online.ticketsSold", is(0)));
    }

    @Test
    void movieAnalytics_returnsZeroesForAMovieWithNoShows() throws Exception {
        String adminToken = loginAsNewAdmin();
        UUID movieId = createMovie(adminToken);

        mockMvc.perform(get("/api/admin/analytics/movies/" + movieId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalShows", is(0)))
                .andExpect(jsonPath("$.ticketsSold", is(0)))
                .andExpect(jsonPath("$.averageOccupancyPercent", is(0.0)));
    }

    @Test
    void occupancyTrend_coversTheDateRangeSortedByShowtime() throws Exception {
        String adminToken = loginAsNewAdmin();
        UUID movieId = createMovie(adminToken);
        LocalDate date = LocalDate.now().plusDays(ThreadLocalRandom.current().nextInt(24, 300));
        UUID showId = createShowOnDate(adminToken, movieId, date, LocalTime.of(14, 0));
        sellCounterTicket(adminToken, showId);

        mockMvc.perform(get("/api/admin/analytics/occupancy")
                        .param("from", date.toString()).param("to", date.toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.showId == '" + showId + "')].ticketsSold", is(List.of(1))))
                .andExpect(jsonPath("$[?(@.showId == '" + showId + "')].totalSeats", is(List.of(300))));
    }

    @Test
    void occupancyTrend_rejectsFromAfterTo() throws Exception {
        String adminToken = loginAsNewAdmin();
        mockMvc.perform(get("/api/admin/analytics/occupancy")
                        .param("from", "2026-06-10").param("to", "2026-06-01")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }

    @Test
    void nonAdminCannotAccessAnalytics() throws Exception {
        String username = "counter-" + UUID.randomUUID();
        seedStaff(username, "a-strong-password", StaffRole.COUNTER_STAFF);
        String token = loginAs(username, "a-strong-password");

        mockMvc.perform(get("/api/admin/analytics/occupancy")
                        .param("from", "2026-06-01").param("to", "2026-06-10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private void checkIn(String adminToken, String bookingId, int tickets) throws Exception {
        mockMvc.perform(post("/api/validation/bookings/" + bookingId + "/check-in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ticketsToAdmit\":" + tickets + "}"))
                .andExpect(status().isOk());
    }

    private String bookingIdFor(String adminToken, String reference) throws Exception {
        String response = mockMvc.perform(get("/api/validation/lookup").param("reference", reference)
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get(0).get("id").asString();
    }

    private String sellCounterTicket(String adminToken, UUID showId) throws Exception {
        UUID seatId = showSeatAllocationRepository.findByShowId(showId).stream()
                .filter(a -> a.getChannel() == AllocationChannel.PHYSICAL)
                .map(ShowSeatAllocation::getId)
                .findFirst().orElseThrow();

        String body = """
                {"showId":"%s","adultCount":1,"childCount":0,"seatAllocationIds":["%s"],
                 "physicalPaymentMethod":"CASH","customerName":"Analytics Test","customerPhone":"+94779990000"}
                """.formatted(showId, seatId);

        String response = mockMvc.perform(post("/api/counter/sales").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("bookingReference").asString();
    }

    private UUID createMovie(String adminToken) throws Exception {
        MovieRequest movieRequest = new MovieRequest("Analytics Test Movie " + UUID.randomUUID(), "Tamil", "Drama", 140, null, null);
        String response = mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movieRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asString());
    }

    private UUID createShow(String adminToken, UUID movieId) throws Exception {
        int dayOffset = ThreadLocalRandom.current().nextInt(24, 300);
        int hour = ThreadLocalRandom.current().nextInt(10, 23);
        return createShowOnDate(adminToken, movieId, LocalDate.now().plusDays(dayOffset), LocalTime.of(hour, 0));
    }

    private UUID createShowOnDate(String adminToken, UUID movieId, LocalDate date, LocalTime time) throws Exception {
        ShowRequest showRequest = new ShowRequest(movieId, date, time,
                new BigDecimal("450.00"), new BigDecimal("300.00"), true, true, 40, true);
        String response = mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asString());
    }
}
