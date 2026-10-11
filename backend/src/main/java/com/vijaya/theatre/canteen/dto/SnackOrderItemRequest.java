package com.vijaya.theatre.canteen.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record SnackOrderItemRequest(
        @NotNull UUID snackItemId,
        @Positive int quantity
) {
}
