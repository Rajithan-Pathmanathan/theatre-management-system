package com.vijaya.theatre.booking;

import com.vijaya.theatre.common.exception.BusinessRuleViolationException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** BR-04 — shared between online (UC-08) and counter (UC-10) sales: seat count must exactly match ticket count. */
public final class TicketSeatCountValidator {

    private TicketSeatCountValidator() {
    }

    public static void validate(int adultCount, int childCount, List<UUID> seatAllocationIds) {
        int ticketCount = adultCount + childCount;
        if (ticketCount <= 0) {
            throw new BusinessRuleViolationException("At least one adult or child ticket is required.");
        }
        Set<UUID> distinctSeatIds = new HashSet<>(seatAllocationIds);
        if (distinctSeatIds.size() != seatAllocationIds.size()) {
            throw new BusinessRuleViolationException("The same seat was selected more than once.");
        }
        if (distinctSeatIds.size() != ticketCount) {
            throw new BusinessRuleViolationException(
                    "Selected seat count (" + distinctSeatIds.size() + ") must match the requested ticket count (" + ticketCount + ").");
        }
    }
}
