package com.vijaya.theatre.canteen.dto;

import com.vijaya.theatre.canteen.PickupTiming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * UC-18, BR-08. Exactly one of {@code bookingId}/{@code showId} drives which
 * show this order belongs to — a cross-field rule validated in the service,
 * not here. {@code bookingId} is optional (BR-08: never required); when
 * given, the show is taken from that booking rather than {@code showId}.
 */
public record SnackOrderRequest(
        UUID bookingId,
        UUID showId,
        @NotBlank String customerName,
        @NotBlank String customerPhone,
        @NotNull PickupTiming pickupTiming,
        @NotEmpty List<SnackOrderItemRequest> items
) {
}
