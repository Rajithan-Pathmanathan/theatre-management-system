package com.vijaya.theatre.show.dto;

import com.vijaya.theatre.show.AllocationChannel;
import com.vijaya.theatre.show.AllocationStatus;
import com.vijaya.theatre.show.SeatZone;

import java.util.UUID;

public record SeatAllocationItemResponse(
        UUID seatId,
        String seatNumber,
        String rowLabel,
        int seatInRow,
        SeatZone zone,
        AllocationChannel channel,
        AllocationStatus status
) {
}
