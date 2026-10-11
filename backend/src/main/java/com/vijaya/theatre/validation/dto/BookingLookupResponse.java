package com.vijaya.theatre.validation.dto;

import com.vijaya.theatre.booking.BookingStatus;
import com.vijaya.theatre.booking.dto.BookedSeatResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** UC-11. What door staff see before deciding how many tickets to admit. */
public record BookingLookupResponse(
        UUID id,
        String bookingReference,
        BookingStatus status,
        String movieTitle,
        LocalDate showDate,
        LocalTime startTime,
        String customerName,
        int adultCount,
        int childCount,
        int ticketsUsed,
        int remainingTickets,
        List<BookedSeatResponse> seats
) {
}
