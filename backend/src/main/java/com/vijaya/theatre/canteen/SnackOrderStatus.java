package com.vijaya.theatre.canteen;

/** UC-19, BR-16 — no automatic expiry for an uncollected order; CLOSED only ever happens via explicit staff action. */
public enum SnackOrderStatus {
    PLACED,
    READY,
    COLLECTED,
    CLOSED
}
