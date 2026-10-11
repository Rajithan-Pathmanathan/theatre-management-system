package com.vijaya.theatre.counter.dto;

import com.vijaya.theatre.booking.PhysicalPaymentMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.UUID;

/**
 * UC-10. {@code seatAllocationIds} are {@code ShowSeatAllocation} ids from
 * {@code GET /api/counter/shows/{showId}/seats} (PHYSICAL channel only), same
 * as the online booking flow. Customer contact details are optional — a
 * walk-in counter sale often has neither (BR-17 only requires the payment
 * method, not customer identity).
 */
public record CounterSaleRequest(
        @NotNull UUID showId,
        @PositiveOrZero int adultCount,
        @PositiveOrZero int childCount,
        @NotEmpty List<UUID> seatAllocationIds,
        @NotNull PhysicalPaymentMethod physicalPaymentMethod,
        String customerName,
        String customerPhone
) {
}
