package com.vijaya.theatre.show.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** UC-07 — only ever represents a show that is actually online-bookable (guaranteed, enabled, scheduled, in the future). */
public record PublicShowResponse(
        UUID id,
        UUID movieId,
        String movieTitle,
        LocalDate showDate,
        LocalTime startTime,
        BigDecimal adultPrice,
        BigDecimal childPrice,
        boolean hasInterval,
        long onlineSeatsRemaining
) {
}
