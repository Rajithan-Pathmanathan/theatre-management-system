package com.vijaya.theatre.booking;

import com.vijaya.theatre.booking.dto.BookingAdminResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** UC-13. ADMIN only — gated by the {@code /api/admin/**} rule in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/bookings")
class BookingAdminController {

    private final BookingService bookingService;

    BookingAdminController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    List<BookingAdminResponse> list(@RequestParam(required = false) UUID showId,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate showDate,
                                     @RequestParam(required = false) BookingStatus status,
                                     @RequestParam(required = false) BookingChannel channel,
                                     @RequestParam(required = false) String reference) {
        return bookingService.listForAdmin(showId, showDate, status, channel, reference);
    }

    @GetMapping("/{bookingId}")
    BookingAdminResponse get(@PathVariable UUID bookingId) {
        return bookingService.getBookingForAdmin(bookingId);
    }
}
