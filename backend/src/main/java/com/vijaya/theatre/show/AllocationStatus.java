package com.vijaya.theatre.show;

/**
 * Per-show seat state. LOCKED is the 10-minute hold taken during an active
 * online booking session (BR-03); it reverts to AVAILABLE automatically if
 * the lock expires before payment completes.
 */
public enum AllocationStatus {
    AVAILABLE,
    LOCKED,
    BOOKED
}
