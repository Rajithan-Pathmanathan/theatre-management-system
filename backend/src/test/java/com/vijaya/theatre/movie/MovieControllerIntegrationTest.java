package com.vijaya.theatre.movie;

import com.vijaya.theatre.AbstractIntegrationTest;
import com.vijaya.theatre.auth.StaffRole;
import com.vijaya.theatre.movie.dto.MovieRequest;
import com.vijaya.theatre.movie.dto.MovieStatusRequest;
import com.vijaya.theatre.show.dto.ShowRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC-04. */
class MovieControllerIntegrationTest extends AbstractIntegrationTest {

    private static final MovieRequest SAMPLE_MOVIE = new MovieRequest(
            "Test Feature", "Tamil", "Drama", 140, null, "A test synopsis.");

    @Test
    void adminCanCreateAndRetrieveAMovie() throws Exception {
        String token = loginAsNewAdmin();
        String movieId = createMovie(token, SAMPLE_MOVIE);

        mockMvc.perform(get("/api/admin/movies/" + movieId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Test Feature")))
                .andExpect(jsonPath("$.status", is("UPCOMING")));
    }

    @Test
    void nonAdminCannotCreateAMovie() throws Exception {
        seedStaff("counter-movie-test", "a-strong-password", StaffRole.COUNTER_STAFF);
        String token = loginAs("counter-movie-test", "a-strong-password");

        mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SAMPLE_MOVIE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivatingAMovie_isBlockedWhileAFutureScheduledShowReferencesIt() throws Exception {
        String token = loginAsNewAdmin();
        String movieId = createMovie(token, SAMPLE_MOVIE);

        ShowRequest show = new ShowRequest(
                UUID.fromString(movieId), LocalDate.now().plusDays(1), LocalTime.of(18, 30),
                new BigDecimal("450.00"), new BigDecimal("300.00"), true, true, 40, true);
        mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(show)))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/admin/movies/" + movieId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MovieStatusRequest(MovieStatus.INACTIVE))))
                .andExpect(status().isConflict());
    }

    @Test
    void deactivatingAMovie_succeedsWhenNoFutureScheduledShowsExist() throws Exception {
        String token = loginAsNewAdmin();
        String movieId = createMovie(token, SAMPLE_MOVIE);

        mockMvc.perform(patch("/api/admin/movies/" + movieId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MovieStatusRequest(MovieStatus.INACTIVE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("INACTIVE")));
    }

    private String createMovie(String token, MovieRequest request) throws Exception {
        String response = mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asString();
    }
}
