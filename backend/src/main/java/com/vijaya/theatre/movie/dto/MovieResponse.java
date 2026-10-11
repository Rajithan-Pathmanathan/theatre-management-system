package com.vijaya.theatre.movie.dto;

import com.vijaya.theatre.movie.MovieStatus;

import java.util.UUID;

public record MovieResponse(
        UUID id,
        String title,
        String language,
        String genre,
        Integer durationMinutes,
        String posterUrl,
        String synopsis,
        MovieStatus status
) {
}
