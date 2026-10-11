package com.vijaya.theatre.show;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ShowSeatAllocationRepository extends JpaRepository<ShowSeatAllocation, UUID> {

    List<ShowSeatAllocation> findByShowId(UUID showId);

    List<ShowSeatAllocation> findByShowIdAndChannel(UUID showId, AllocationChannel channel);

    long countByShowIdAndChannelAndStatus(UUID showId, AllocationChannel channel, AllocationStatus status);

    /** BR-03 lock-expiry sweep (SeatLockExpirySweep). */
    List<ShowSeatAllocation> findByStatusAndLockedUntilBefore(AllocationStatus status, Instant before);

    /**
     * FR-12, NFR-02 — the actual double-booking-prevention mechanism. Takes a
     * real {@code SELECT ... FOR UPDATE} row lock on exactly these rows, for
     * the lifetime of the caller's transaction; a second concurrent call
     * requesting any of the same ids blocks until the first transaction
     * commits or rolls back, then sees the post-transaction state. The
     * {@code ORDER BY id} is deliberate — always acquiring multi-row locks in
     * the same order avoids a classic lock-ordering deadlock between two
     * requests that both contend for two overlapping seats.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ShowSeatAllocation a where a.id in :ids order by a.id")
    List<ShowSeatAllocation> lockAllByIdForUpdate(@Param("ids") List<UUID> ids);
}
