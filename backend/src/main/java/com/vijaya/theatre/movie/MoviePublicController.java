package com.vijaya.theatre.movie;

import com.vijaya.theatre.movie.dto.MovieResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** UC-07. Public — gated as permitAll in SecurityConfig, not under /api/admin. */
@RestController
@RequestMapping("/api/movies")
class MoviePublicController {

    private final MovieService movieService;

    MoviePublicController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    List<MovieResponse> list() {
        return movieService.listPublic();
    }
}
