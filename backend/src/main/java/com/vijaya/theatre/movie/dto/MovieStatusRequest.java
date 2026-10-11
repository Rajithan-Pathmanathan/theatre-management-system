package com.vijaya.theatre.movie.dto;

import com.vijaya.theatre.movie.MovieStatus;
import jakarta.validation.constraints.NotNull;

/** FR-03. */
public record MovieStatusRequest(@NotNull MovieStatus status) {
}
