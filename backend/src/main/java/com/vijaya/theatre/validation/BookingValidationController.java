package com.vijaya.theatre.validation;

import com.vijaya.theatre.auth.AuthenticatedStaff;
import com.vijaya.theatre.validation.dto.BookingLookupResponse;
import com.vijaya.theatre.validation.dto.CheckInRequest;
import com.vijaya.theatre.validation.dto.CheckInResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** UC-11. COUNTER_STAFF/ADMIN only — gated in SecurityConfig. */
@RestController
@RequestMapping("/api/validation")
class BookingValidationController {

    private final BookingValidationService bookingValidationService;

    BookingValidationController(BookingValidationService bookingValidationService) {
        this.bookingValidationService = bookingValidationService;
    }

    @GetMapping("/lookup")
    List<BookingLookupResponse> lookup(@RequestParam(required = false) String reference,
                                        @RequestParam(required = false) UUID qrToken,
                                        @RequestParam(required = false) String phone) {
        return bookingValidationService.lookup(reference, qrToken, phone);
    }

    @PostMapping("/bookings/{bookingId}/check-in")
    CheckInResponse checkIn(@AuthenticationPrincipal AuthenticatedStaff principal,
                             @PathVariable UUID bookingId,
                             @Valid @RequestBody CheckInRequest request) {
        return bookingValidationService.checkIn(principal.staffId(), bookingId, request);
    }
}
