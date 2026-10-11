package com.vijaya.theatre.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** UC-14, FR-33–FR-39. Counts only CONFIRMED bookings — a real, realized sale. */
public record ShowReportResponse(
        UUID showId,
        String movieTitle,
        LocalDate showDate,
        LocalTime startTime,
        int ticketsSold,
        BigDecimal collections,
        ChannelBreakdown online,
        ChannelBreakdown physical
) {
}
