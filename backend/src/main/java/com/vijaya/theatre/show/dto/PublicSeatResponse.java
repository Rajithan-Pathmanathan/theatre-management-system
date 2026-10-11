package com.vijaya.theatre.show.dto;

import com.vijaya.theatre.show.AllocationStatus;
import com.vijaya.theatre.show.SeatZone;

import java.util.UUID;

/**
 * FR-18 — only ONLINE-channel seats are ever exposed here; a customer never
 * sees the PHYSICAL-channel seats reserved for the counter. {@code seatId}
 * is the {@code ShowSeatAllocation} id — pass it back as-is in a booking
 * request's {@code seatAllocationIds}.
 */
public record PublicSeatResponse(
        UUID seatId,
        String seatNumber,
        String rowLabel,
        int seatInRow,
        SeatZone zone,
        AllocationStatus status
) {
}
