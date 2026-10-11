package com.vijaya.theatre.show.dto;

import com.vijaya.theatre.show.ShowStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ShowResponse(
        UUID id,
        UUID movieId,
        String movieTitle,
        LocalDate showDate,
        LocalTime startTime,
        ShowStatus status,
        BigDecimal adultPrice,
        BigDecimal childPrice,
        boolean guaranteed,
        boolean onlineBookingEnabled,
        int onlineAllocationPercent,
        boolean hasInterval
) {
}
