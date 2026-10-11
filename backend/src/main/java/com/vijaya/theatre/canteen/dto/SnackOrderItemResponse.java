package com.vijaya.theatre.canteen.dto;

import java.math.BigDecimal;

public record SnackOrderItemResponse(
        String itemName,
        BigDecimal unitPrice,
        int quantity,
        boolean unavailable
) {
}
