package com.vijaya.theatre.booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.UUID;

/**
 * UC-08. {@code seatAllocationIds} are the ids from {@code GET /api/shows/{id}/seats}
 * (the per-show {@code ShowSeatAllocation} rows), not raw seat ids — their
 * count must equal {@code adultCount + childCount} (BR-04, validated in the
 * service, not here, since it's a cross-field rule).
 */
public record BookingRequest(
        @NotNull UUID showId,
        @PositiveOrZero int adultCount,
        @PositiveOrZero int childCount,
        @NotEmpty List<UUID> seatAllocationIds,
        @NotBlank String customerName,
        @NotBlank String customerPhone,
        @Email String customerEmail
) {
}
