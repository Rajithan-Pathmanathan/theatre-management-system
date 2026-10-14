package com.vijaya.theatre.canteen.dto;

import com.vijaya.theatre.canteen.PickupTiming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** BR-13. The snacks to pay for with a booking's tickets; an empty list removes them. */
public record BookingSnackRequest(
        @NotNull PickupTiming pickupTiming,
        @NotNull List<@Valid SnackOrderItemRequest> items
) {
}
