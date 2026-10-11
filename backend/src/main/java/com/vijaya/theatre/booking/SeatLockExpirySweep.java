package com.vijaya.theatre.booking;

import com.vijaya.theatre.show.AllocationStatus;
import com.vijaya.theatre.show.ShowSeatAllocation;
import com.vijaya.theatre.show.ShowSeatAllocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * BR-03, UC-08 alternative flow A4. {@link BookingService} already reclaims
 * an expired LOCKED seat lazily the moment someone else tries to book it, so
 * this sweep isn't what makes seats bookable again — it's what makes the
 * *data* correct for everyone else: the seat map for someone just browsing
 * (not actively contending for that seat) and the abandoned booking's own
 * status, which would otherwise sit at PENDING_PAYMENT forever.
 */
@Component
class SeatLockExpirySweep {

    private static final Logger log = LoggerFactory.getLogger(SeatLockExpirySweep.class);

    private final ShowSeatAllocationRepository showSeatAllocationRepository;
    private final BookingRepository bookingRepository;

    SeatLockExpirySweep(ShowSeatAllocationRepository showSeatAllocationRepository, BookingRepository bookingRepository) {
        this.showSeatAllocationRepository = showSeatAllocationRepository;
        this.bookingRepository = bookingRepository;
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    void releaseExpiredLocks() {
        Instant now = Instant.now();

        List<ShowSeatAllocation> expiredAllocations =
                showSeatAllocationRepository.findByStatusAndLockedUntilBefore(AllocationStatus.LOCKED, now);
        expiredAllocations.forEach(allocation -> {
            allocation.setStatus(AllocationStatus.AVAILABLE);
            allocation.setLockedUntil(null);
            allocation.setLockToken(null);
        });
        showSeatAllocationRepository.saveAll(expiredAllocations);

        List<Booking> expiredBookings = bookingRepository.findByStatusAndExpiresAtBefore(BookingStatus.PENDING_PAYMENT, now);
        expiredBookings.forEach(booking -> booking.setStatus(BookingStatus.EXPIRED));
        bookingRepository.saveAll(expiredBookings);

        if (!expiredAllocations.isEmpty() || !expiredBookings.isEmpty()) {
            log.info("Released {} expired seat lock(s), expired {} pending booking(s)",
                    expiredAllocations.size(), expiredBookings.size());
        }
    }
}
