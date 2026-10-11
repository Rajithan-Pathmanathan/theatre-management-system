package com.vijaya.theatre.counter;

import com.vijaya.theatre.auth.AuthenticatedStaff;
import com.vijaya.theatre.booking.dto.BookingResponse;
import com.vijaya.theatre.counter.dto.CounterSaleRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** UC-10. COUNTER_STAFF/ADMIN only — gated in SecurityConfig. */
@RestController
@RequestMapping("/api/counter")
class CounterSaleController {

    private final CounterSaleService counterSaleService;

    CounterSaleController(CounterSaleService counterSaleService) {
        this.counterSaleService = counterSaleService;
    }

    @PostMapping("/sales")
    ResponseEntity<BookingResponse> sell(@AuthenticationPrincipal AuthenticatedStaff principal,
                                          @Valid @RequestBody CounterSaleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(counterSaleService.sellTicket(principal.staffId(), request));
    }
}
