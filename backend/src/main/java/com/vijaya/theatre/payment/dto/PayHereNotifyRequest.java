package com.vijaya.theatre.payment.dto;

/**
 * UC-09. The subset of PayHere's IPN (server-to-server notify) fields we
 * actually need. Deliberately excludes card_no/card_holder_name/card_expiry —
 * NFR-07 means we never touch payment-instrument data, not even in a webhook.
 */
public record PayHereNotifyRequest(
        String merchantId,
        String orderId,
        String paymentId,
        String payhereAmount,
        String payhereCurrency,
        String statusCode,
        String md5sig
) {
}
