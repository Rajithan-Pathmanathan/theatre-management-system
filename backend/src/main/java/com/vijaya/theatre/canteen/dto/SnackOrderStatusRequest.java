package com.vijaya.theatre.canteen.dto;

import com.vijaya.theatre.canteen.SnackOrderStatus;
import jakarta.validation.constraints.NotNull;

public record SnackOrderStatusRequest(@NotNull SnackOrderStatus status) {
}
