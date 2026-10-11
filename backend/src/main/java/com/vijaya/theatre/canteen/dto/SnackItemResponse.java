package com.vijaya.theatre.canteen.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SnackItemResponse(
        UUID id,
        String name,
        BigDecimal price,
        boolean available
) {
}
