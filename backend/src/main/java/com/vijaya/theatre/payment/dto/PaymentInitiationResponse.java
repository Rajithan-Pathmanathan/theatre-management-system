package com.vijaya.theatre.payment.dto;

/**
 * UC-09. Everything the frontend needs to build and submit PayHere's hosted
 * checkout form. {@code hash} is server-computed (requires the merchant
 * secret, which must never reach the browser) — see PayHereHashUtil.
 */
public record PaymentInitiationResponse(
        String checkoutUrl,
        String merchantId,
        String orderId,
        String itemsDescription,
        String amount,
        String currency,
        String hash,
        String firstName,
        String lastName,
        String email,
        String phone,
        String address,
        String city,
        String country,
        String returnUrl,
        String cancelUrl,
        String notifyUrl
) {
}
