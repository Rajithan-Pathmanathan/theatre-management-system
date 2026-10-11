package com.vijaya.theatre.show.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** UC-05 — FR-04 (create) and FR-08 (update) share this shape; cancel/disable go through {@link ShowStatusRequest}. */
public record ShowRequest(
        @NotNull UUID movieId,
        @NotNull LocalDate showDate,
        @NotNull LocalTime startTime,
        @NotNull @PositiveOrZero BigDecimal adultPrice,
        @NotNull @PositiveOrZero BigDecimal childPrice,
        boolean guaranteed,
        boolean onlineBookingEnabled,
        @Min(0) @Max(100) int onlineAllocationPercent,
        boolean hasInterval
) {
}
