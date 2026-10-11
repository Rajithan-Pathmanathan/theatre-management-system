package com.vijaya.theatre.canteen.dto;

import com.vijaya.theatre.canteen.PickupTiming;
import com.vijaya.theatre.canteen.SnackOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record SnackOrderResponse(
        UUID id,
        LocalDate orderDate,
        int orderNumber,
        UUID showId,
        String movieTitle,
        LocalDate showDate,
        LocalTime startTime,
        String customerName,
        String customerPhone,
        PickupTiming pickupTiming,
        SnackOrderStatus status,
        BigDecimal totalAmount,
        List<SnackOrderItemResponse> items,
        String linkedBookingReference
) {
}
