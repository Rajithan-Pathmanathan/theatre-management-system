package com.vijaya.theatre.booking;

import com.vijaya.theatre.booking.dto.BookingRequest;
import com.vijaya.theatre.booking.dto.BookingResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** UC-08/UC-09. Public — guest checkout, no account needed; gated as permitAll in SecurityConfig. */
@RestController
@RequestMapping("/api/bookings")
class BookingController {

    private final BookingService bookingService;

    BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(request));
    }

    /** UC-09 polling — lets the payment-result page check whether the booking has been confirmed yet. */
    @GetMapping("/{bookingId}")
    BookingResponse get(@PathVariable UUID bookingId) {
        return bookingService.getBooking(bookingId);
    }
}
