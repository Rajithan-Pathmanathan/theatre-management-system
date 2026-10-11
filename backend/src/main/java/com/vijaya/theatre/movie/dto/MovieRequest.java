package com.vijaya.theatre.movie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** UC-04 — FR-01 (create) and FR-02 (update) share this shape; status changes go through {@link MovieStatusRequest}. */
public record MovieRequest(
        @NotBlank String title,
        @NotBlank String language,
        @NotBlank String genre,
        @NotNull @Positive Integer durationMinutes,
        String posterUrl,
        String synopsis
) {
}
