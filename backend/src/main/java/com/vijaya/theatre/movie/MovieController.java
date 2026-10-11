package com.vijaya.theatre.movie;

import com.vijaya.theatre.movie.dto.MovieRequest;
import com.vijaya.theatre.movie.dto.MovieResponse;
import com.vijaya.theatre.movie.dto.MovieStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** UC-04. ADMIN only — gated by the {@code /api/admin/**} rule in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/movies")
class MovieController {

    private final MovieService movieService;

    MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    List<MovieResponse> list() {
        return movieService.list();
    }

    @GetMapping("/{id}")
    MovieResponse get(@PathVariable UUID id) {
        return movieService.get(id);
    }

    @PostMapping
    ResponseEntity<MovieResponse> create(@Valid @RequestBody MovieRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(movieService.create(request));
    }

    @PutMapping("/{id}")
    MovieResponse update(@PathVariable UUID id, @Valid @RequestBody MovieRequest request) {
        return movieService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    MovieResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody MovieStatusRequest request) {
        return movieService.updateStatus(id, request);
    }
}
