package com.vijaya.theatre.analytics;

import com.vijaya.theatre.analytics.dto.MovieAnalyticsResponse;
import com.vijaya.theatre.analytics.dto.ShowOccupancyResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** UC-15. ADMIN only — gated by the {@code /api/admin/**} rule in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/analytics")
class AnalyticsController {

    private final AnalyticsService analyticsService;

    AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/movies/{movieId}")
    MovieAnalyticsResponse movie(@PathVariable UUID movieId) {
        return analyticsService.getMovieAnalytics(movieId);
    }

    @GetMapping("/occupancy")
    List<ShowOccupancyResponse> occupancy(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analyticsService.getOccupancyTrend(from, to);
    }
}
