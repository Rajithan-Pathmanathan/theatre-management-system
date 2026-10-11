package com.vijaya.theatre.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DailyReportResponse(
        LocalDate date,
        int totalTicketsSold,
        BigDecimal totalCollections,
        ChannelBreakdown online,
        ChannelBreakdown physical,
        List<ShowReportResponse> shows
) {
}
