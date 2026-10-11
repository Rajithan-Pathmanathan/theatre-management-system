package com.vijaya.theatre.payment;

import com.vijaya.theatre.payment.dto.PayHereNotifyRequest;
import com.vijaya.theatre.payment.dto.PaymentInitiationResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** UC-09. Public — guest checkout and PayHere's own server both call these with no staff token. */
@RestController
class PaymentController {

    private final PaymentService paymentService;

    PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/bookings/{bookingId}/payment")
    PaymentInitiationResponse initiate(@PathVariable UUID bookingId) {
        return paymentService.initiatePayment(bookingId);
    }

    /** PayHere's IPN callback. Field names are theirs (snake_case), bound explicitly rather than via a form-backed DTO. */
    @PostMapping(value = "/api/payments/payhere/notify", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    ResponseEntity<Void> notify(@RequestParam("merchant_id") String merchantId,
                                @RequestParam("order_id") String orderId,
                                @RequestParam("payment_id") String paymentId,
                                @RequestParam("payhere_amount") String payhereAmount,
                                @RequestParam("payhere_currency") String payhereCurrency,
                                @RequestParam("status_code") String statusCode,
                                @RequestParam("md5sig") String md5sig) {
        paymentService.handleNotification(new PayHereNotifyRequest(
                merchantId, orderId, paymentId, payhereAmount, payhereCurrency, statusCode, md5sig));
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/api/bookings/{bookingId}/qr-code", produces = MediaType.IMAGE_PNG_VALUE)
    ResponseEntity<byte[]> qrCode(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(paymentService.getQrCode(bookingId));
    }
}
