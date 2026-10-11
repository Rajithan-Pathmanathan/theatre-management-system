package com.vijaya.theatre.booking;

/**
 * Online bookings start PENDING_PAYMENT and only become CONFIRMED after a
 * successful payment callback (BR-02) — never on a pay-later basis. Counter
 * bookings are created directly as CONFIRMED (cash/card is settled on the
 * spot). EXPIRED is the lock-timeout outcome for an online booking.
 */
public enum BookingStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    EXPIRED,
    CANCELLED,
    REFUNDED
}
