package com.vijaya.theatre.validation.dto;

import java.time.Instant;
import java.util.UUID;

public record CheckInResponse(
        UUID bookingId,
        String bookingReference,
        int ticketsAdmittedThisEntry,
        int ticketsUsed,
        int remainingTickets,
        boolean fullyCheckedIn,
        Instant validatedAt
) {
}
