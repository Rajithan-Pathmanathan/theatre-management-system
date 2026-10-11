package com.vijaya.theatre.canteen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** UC-17 — create and update share this shape; availability toggles through {@link SnackItemAvailabilityRequest}. */
public record SnackItemRequest(
        @NotBlank String name,
        @NotNull @PositiveOrZero BigDecimal price
) {
}
