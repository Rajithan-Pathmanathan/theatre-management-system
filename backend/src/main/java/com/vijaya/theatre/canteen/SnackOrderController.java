package com.vijaya.theatre.canteen;

import com.vijaya.theatre.canteen.dto.BookingSnackRequest;
import com.vijaya.theatre.canteen.dto.SnackOrderRequest;
import com.vijaya.theatre.canteen.dto.SnackOrderResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/** UC-18, BR-08. Public — no booking or account needed; gated as permitAll in SecurityConfig. */
@RestController
@RequestMapping("/api/snack-orders")
class SnackOrderController {

    private final SnackOrderService snackOrderService;

    SnackOrderController(SnackOrderService snackOrderService) {
        this.snackOrderService = snackOrderService;
    }

    @PostMapping
    ResponseEntity<SnackOrderResponse> create(@Valid @RequestBody SnackOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(snackOrderService.createOrder(request));
    }

    /** BR-13 — the snacks to pay for with this booking's tickets; 204 when the list was empty. */
    @PutMapping("/booking/{bookingId}")
    ResponseEntity<SnackOrderResponse> setBookingSnacks(@PathVariable UUID bookingId, @Valid @RequestBody BookingSnackRequest request) {
        SnackOrderResponse order = snackOrderService.setBookingSnacks(bookingId, request);
        return order == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(order);
    }

    @GetMapping("/{id}")
    SnackOrderResponse get(@PathVariable UUID id) {
        return snackOrderService.getById(id);
    }

    /**
     * UC-12-style self-service lookup by the human-facing order number, for a customer who didn't keep the link.
     * Needs the phone the order was placed with; a missing phone is a 404 like any other mismatch.
     */
    @GetMapping("/lookup")
    SnackOrderResponse lookup(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                              @RequestParam int orderNumber,
                              @RequestParam(required = false) String phone) {
        return snackOrderService.getByOrderNumber(date, orderNumber, phone);
    }

    @GetMapping(value = "/{id}/qr-code", produces = MediaType.IMAGE_PNG_VALUE)
    ResponseEntity<byte[]> qrCode(@PathVariable UUID id) {
        return ResponseEntity.ok(snackOrderService.getQrCode(id));
    }
}
