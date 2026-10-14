package com.vijaya.theatre.canteen.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SnackOrderItemResponse(
        UUID id,
        String itemName,
        BigDecimal unitPrice,
        int quantity,
        boolean unavailable
) {
}
