package com.vijaya.theatre.booking;

/** Payment method recorded for a counter sale (BR-17). Null for online bookings — those carry a {@code PaymentTransaction} instead. */
public enum PhysicalPaymentMethod {
    CASH,
    CARD,
    OTHER
}
