package com.vijaya.theatre.show.dto;

import java.util.List;
import java.util.UUID;

public record SeatAllocationSummaryResponse(
        UUID showId,
        int onlineAllocationPercent,
        int totalSeats,
        long onlineCount,
        long physicalCount,
        List<SeatAllocationItemResponse> seats
) {
}
