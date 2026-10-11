package com.vijaya.theatre.payment;

/** PayHere is the primary gateway (LK context); Stripe test mode is the documented fallback for dev. */
public enum PaymentGateway {
    PAYHERE,
    STRIPE
}
