package com.vijaya.theatre.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
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

    /** UC-14 daily report — the realized (CONFIRMED) sales for a given show date. */
    List<Booking> findByShow_ShowDateAndStatus(LocalDate showDate, BookingStatus status);

    /** UC-14 show-wise report — the realized (CONFIRMED) sales for a given show. */
    List<Booking> findByShowIdAndStatus(UUID showId, BookingStatus status);

    /** UC-13 — admin booking list/search; every filter is optional. */
    @Query("""
            select b from Booking b
            where (:showId is null or b.show.id = :showId)
              and (:showDate is null or b.show.showDate = :showDate)
              and (:status is null or b.status = :status)
              and (:channel is null or b.channel = :channel)
              and (:reference is null or b.bookingReference = :reference)
            order by b.createdAt desc
            """)
    List<Booking> search(@Param("showId") UUID showId, @Param("showDate") LocalDate showDate,
                          @Param("status") BookingStatus status, @Param("channel") BookingChannel channel,
                          @Param("reference") String reference);
}
