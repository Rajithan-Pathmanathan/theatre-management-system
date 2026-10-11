package com.vijaya.theatre.show;

import com.vijaya.theatre.AbstractIntegrationTest;
import com.vijaya.theatre.auth.StaffRole;
import com.vijaya.theatre.movie.dto.MovieRequest;
import com.vijaya.theatre.show.dto.ShowRequest;
import com.vijaya.theatre.show.dto.ShowStatusRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC-05 — the slot-overlap check (alternative flow A1) and BR-01 enforcement are the risk-critical logic here. */
class ShowControllerIntegrationTest extends AbstractIntegrationTest {

    private static final LocalDate SHOW_DATE = LocalDate.now().plusDays(7);

    @Test
    void adminCanCreateAGuaranteedShow() throws Exception {
        String token = loginAsNewAdmin();
        UUID movieId = createMovie(token, 140);

        mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showRequest(movieId, LocalTime.of(18, 30), true, true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("SCHEDULED")))
                .andExpect(jsonPath("$.guaranteed", is(true)));
    }

    @Test
    void onlineBookingWithoutGuaranteed_violatesBR01() throws Exception {
        String token = loginAsNewAdmin();
        UUID movieId = createMovie(token, 140);

        mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        // onlineBookingEnabled=true but guaranteed=false
                        .content(objectMapper.writeValueAsString(showRequest(movieId, LocalTime.of(18, 30), false, true))))
                .andExpect(status().isConflict());
    }

    @Test
    void overlappingShowOnTheSameDate_isRejected_evenForADifferentMovie() throws Exception {
        String token = loginAsNewAdmin();
        UUID firstMovie = createMovie(token, 150); // 2h30m — 18:30 to 21:00
        UUID secondMovie = createMovie(token, 120);

        mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showRequest(firstMovie, LocalTime.of(18, 30), true, false))))
                .andExpect(status().isCreated());

        // Starts at 20:00, while the first show is still running (ends 21:00) — must overlap.
        mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showRequest(secondMovie, LocalTime.of(20, 0), true, false))))
                .andExpect(status().isConflict());
    }

    @Test
    void backToBackShowsWithNoGap_doNotOverlap() throws Exception {
        String token = loginAsNewAdmin();
        UUID firstMovie = createMovie(token, 120); // 10:00 to 12:00
        UUID secondMovie = createMovie(token, 120);

        mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showRequest(firstMovie, LocalTime.of(10, 0), true, false))))
                .andExpect(status().isCreated());

        // Starts exactly when the first show ends (12:00) — not an overlap.
        mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showRequest(secondMovie, LocalTime.of(12, 0), true, false))))
                .andExpect(status().isCreated());
    }

    @Test
    void cancellingAShow_updatesItsStatus() throws Exception {
        String token = loginAsNewAdmin();
        UUID movieId = createMovie(token, 140);

        String response = mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showRequest(movieId, LocalTime.of(18, 30), true, false))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String showId = objectMapper.readTree(response).get("id").asString();

        mockMvc.perform(patch("/api/admin/shows/" + showId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ShowStatusRequest(ShowStatus.CANCELLED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    void nonAdminCannotCreateAShow() throws Exception {
        seedStaff("counter-show-test", "a-strong-password", StaffRole.COUNTER_STAFF);
        String token = loginAs("counter-show-test", "a-strong-password");
        UUID movieId = createMovie(loginAsNewAdmin(), 140);

        mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showRequest(movieId, LocalTime.of(18, 30), true, false))))
                .andExpect(status().isForbidden());
    }

    private ShowRequest showRequest(UUID movieId, LocalTime startTime, boolean guaranteed, boolean onlineBookingEnabled) {
        return new ShowRequest(movieId, SHOW_DATE, startTime,
                new BigDecimal("450.00"), new BigDecimal("300.00"), guaranteed, onlineBookingEnabled, 40, true);
    }

    private UUID createMovie(String adminToken, int durationMinutes) throws Exception {
        MovieRequest request = new MovieRequest("Test Movie " + UUID.randomUUID(), "Tamil", "Drama", durationMinutes, null, null);
        String response = mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asString());
    }
}
