package com.vijaya.theatre.analytics.dto;

import com.vijaya.theatre.reporting.dto.ChannelBreakdown;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * UC-15, FR-40–FR-45. A movie's performance across every show it has ever
 * had — historical decision-support, not a forecast. Only CONFIRMED
 * bookings count, same convention as the UC-14 daily/show-wise report.
 */
public record MovieAnalyticsResponse(
        UUID movieId,
        String movieTitle,
        int totalShows,
        int ticketsSold,
        BigDecimal collections,
        double averageOccupancyPercent,
        int ticketsUsed,
        double attendanceRatePercent,
        ChannelBreakdown online,
        ChannelBreakdown physical
) {
}
