package com.vijaya.theatre.canteen;

import com.vijaya.theatre.canteen.dto.SnackOrderItemAvailabilityRequest;
import com.vijaya.theatre.canteen.dto.SnackOrderResponse;
import com.vijaya.theatre.canteen.dto.SnackOrderStatusRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** UC-19. CANTEEN_STAFF/ADMIN only — gated in SecurityConfig. */
@RestController
@RequestMapping("/api/canteen/orders")
class CanteenStaffController {

    private final SnackOrderService snackOrderService;

    CanteenStaffController(SnackOrderService snackOrderService) {
        this.snackOrderService = snackOrderService;
    }

    @GetMapping
    List<SnackOrderResponse> queue(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                    @RequestParam(required = false) PickupTiming pickupTiming,
                                    @RequestParam(required = false) SnackOrderStatus status) {
        return snackOrderService.listQueue(date, pickupTiming, status);
    }

    /** UC-19 — verify pickup by scanning the order's QR. */
    @GetMapping("/lookup")
    SnackOrderResponse lookupByToken(@RequestParam UUID orderToken) {
        return snackOrderService.getByOrderToken(orderToken);
    }

    @PatchMapping("/{id}/status")
    SnackOrderResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody SnackOrderStatusRequest request) {
        return snackOrderService.updateStatus(id, request.status());
    }

    @PatchMapping("/{orderId}/items/{itemId}/availability")
    SnackOrderResponse updateItemAvailability(@PathVariable UUID orderId, @PathVariable UUID itemId,
                                               @Valid @RequestBody SnackOrderItemAvailabilityRequest request) {
        return snackOrderService.updateItemAvailability(orderId, itemId, request.unavailable());
    }
}
