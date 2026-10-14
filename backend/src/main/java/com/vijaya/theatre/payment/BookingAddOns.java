package com.vijaya.theatre.payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * BR-13. Extras (canteen snacks) charged in the same online payment as a booking's tickets.
 * Defined here and implemented by the add-on's own module, so the payment core never depends
 * on the canteen (NFR-17): without an implementation, a booking is charged for tickets only.
 */
public interface BookingAddOns {

    /** Called when payment starts: marks the booking's pending extras as charged now and returns their total. */
    BigDecimal chargeWithBooking(UUID bookingId);

    /** Called once the booking's payment succeeds, in the same transaction that confirms it. */
    void bookingPaid(UUID bookingId);
}
