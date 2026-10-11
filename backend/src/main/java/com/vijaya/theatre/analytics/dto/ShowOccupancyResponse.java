package com.vijaya.theatre.analytics.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** UC-15 — one row of the occupancy/attendance trend over a date range. */
public record ShowOccupancyResponse(
        UUID showId,
        String movieTitle,
        LocalDate showDate,
        LocalTime startTime,
        int totalSeats,
        int ticketsSold,
        double occupancyPercent,
        int ticketsUsed,
        double attendanceRatePercent
) {
}
