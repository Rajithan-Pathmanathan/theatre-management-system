package com.vijaya.theatre.booking.dto;

import com.vijaya.theatre.booking.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * UC-08. Still PENDING_PAYMENT at this point — payment and the booking
 * reference/QR becoming final happen in UC-09, the next slice.
 * {@code expiresAt} is what the "complete payment within 10 minutes" UI
 * countdown is driven by (BR-03).
 */
public record BookingResponse(
        UUID id,
        String bookingReference,
        BookingStatus status,
        UUID showId,
        String movieTitle,
        LocalDate showDate,
        LocalTime startTime,
        int adultCount,
        int childCount,
        BigDecimal totalAmount,
        List<BookedSeatResponse> seats,
        Instant expiresAt
) {
}
