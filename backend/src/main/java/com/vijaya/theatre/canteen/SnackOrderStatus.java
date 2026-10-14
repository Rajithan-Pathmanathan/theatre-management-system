package com.vijaya.theatre.canteen;

/** UC-19, BR-16 — no automatic expiry for an uncollected order; CLOSED only ever happens via explicit staff action. */
public enum SnackOrderStatus {
    /** BR-13 — added on the booking's payment page; joins the queue once the booking is paid. */
    AWAITING_PAYMENT,
    PLACED,
    READY,
    COLLECTED,
    CLOSED
}
