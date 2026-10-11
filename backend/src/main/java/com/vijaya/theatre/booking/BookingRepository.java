package com.vijaya.theatre.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    /** UC-12, BR-11 — primary lookup via the printed/QR'd reference. */
    Optional<Booking> findByBookingReference(String bookingReference);

    /** UC-11, BR-11 — fallback lookup when QR scanning is unavailable. */
    Optional<Booking> findByQrToken(UUID qrToken);

    /** UC-11, BR-11 — fallback lookup by the customer's phone number (may return several bookings). */
    List<Booking> findByCustomerPhone(String customerPhone);

    /** BR-03 lock-expiry sweep (SeatLockExpirySweep) — UC-08 alternative flow A4. */
    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, Instant before);
}
