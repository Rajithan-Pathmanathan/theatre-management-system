package com.vijaya.theatre.booking.dto;

import com.vijaya.theatre.booking.BookingChannel;
import com.vijaya.theatre.booking.BookingStatus;
import com.vijaya.theatre.booking.PhysicalPaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** UC-13 — the full admin view of a booking, across both channels. */
public record BookingAdminResponse(
        UUID id,
        String bookingReference,
        BookingStatus status,
        BookingChannel channel,
        UUID showId,
        String movieTitle,
        LocalDate showDate,
        LocalTime startTime,
        String customerName,
        String customerPhone,
        String customerEmail,
        int adultCount,
        int childCount,
        BigDecimal totalAmount,
        int ticketsUsed,
        int remainingTickets,
        PhysicalPaymentMethod physicalPaymentMethod,
        String soldByStaffUsername,
        List<BookedSeatResponse> seats,
        Instant confirmedAt,
        Instant createdAt
) {
}
