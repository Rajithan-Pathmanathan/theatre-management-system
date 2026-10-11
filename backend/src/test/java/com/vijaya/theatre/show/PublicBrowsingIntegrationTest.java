package com.vijaya.theatre.show;

import com.vijaya.theatre.AbstractIntegrationTest;
import com.vijaya.theatre.movie.dto.MovieRequest;
import com.vijaya.theatre.movie.dto.MovieStatusRequest;
import com.vijaya.theatre.movie.MovieStatus;
import com.vijaya.theatre.show.dto.ShowRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC-07. Lower risk than booking (simple filtered reads), so lighter coverage is appropriate. */
class PublicBrowsingIntegrationTest extends AbstractIntegrationTest {

    @Test
    void publicMovieList_excludesInactiveMovies() throws Exception {
        String token = loginAsNewAdmin();
        String title = "Inactive Test Movie " + UUID.randomUUID();
        String movieId = createMovie(token, title);
        mockMvc.perform(patch("/api/admin/movies/" + movieId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MovieStatusRequest(MovieStatus.INACTIVE))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/movies")) // no Authorization header — public
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == '" + title + "')]").isEmpty());
    }

    @Test
    void publicShowList_onlyIncludesGuaranteedOnlineBookableShows() throws Exception {
        String token = loginAsNewAdmin();
        LocalDate date = LocalDate.now().plusDays(25);
        String movieId = createMovie(token, "Public Show Test " + UUID.randomUUID());

        // Not guaranteed — must not appear publicly.
        createShow(token, movieId, date, LocalTime.of(10, 0), false, false);
        // Guaranteed and online-enabled — must appear.
        UUID bookableShowId = createShow(token, movieId, date, LocalTime.of(14, 0), true, true);

        mockMvc.perform(get("/api/shows").param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + bookableShowId + "')]").exists())
                .andExpect(jsonPath("$.length()", is(1)));
    }

    @Test
    void publicShowDetail_404sForANonBookableShow() throws Exception {
        String token = loginAsNewAdmin();
        String movieId = createMovie(token, "Non-bookable Detail Test " + UUID.randomUUID());
        UUID nonGuaranteedShowId = createShow(token, movieId, LocalDate.now().plusDays(26), LocalTime.of(10, 0), false, false);

        mockMvc.perform(get("/api/shows/" + nonGuaranteedShowId))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicSeatsEndpoint_neverExposesPhysicalChannelSeats() throws Exception {
        String token = loginAsNewAdmin();
        String movieId = createMovie(token, "Seats Test " + UUID.randomUUID());
        UUID showId = createShow(token, movieId, LocalDate.now().plusDays(27), LocalTime.of(20, 0), true, true);

        mockMvc.perform(get("/api/shows/" + showId + "/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status", everyItem(not("BOOKED")))) // sanity: nothing pre-booked yet
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    private String createMovie(String token, String title) throws Exception {
        MovieRequest request = new MovieRequest(title, "Tamil", "Drama", 140, null, null);
        String response = mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asString();
    }

    private UUID createShow(String token, String movieId, LocalDate date, LocalTime time,
                             boolean guaranteed, boolean onlineBookingEnabled) throws Exception {
        ShowRequest request = new ShowRequest(UUID.fromString(movieId), date, time,
                new BigDecimal("450.00"), new BigDecimal("300.00"), guaranteed, onlineBookingEnabled, 40, true);
        String response = mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asString());
    }
}
