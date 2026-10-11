package com.vijaya.theatre.show;

import com.vijaya.theatre.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * FR-09–FR-12, FR-19; BR-03–BR-06. One row per (show, seat) — the single
 * source of truth for seat availability and the most concurrency-sensitive
 * table in the system. {@code version} backs optimistic locking as a second
 * line of defence; the booking service's primary mechanism is a pessimistic
 * {@code SELECT ... FOR UPDATE} on this row inside a transaction (see
 * DEVELOPMENT_PLAN.md §2, "Seat locking").
 */
@Entity
@Table(name = "show_seat_allocations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"show_id", "seat_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ShowSeatAllocation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AllocationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AllocationStatus status = AllocationStatus.AVAILABLE;

    /** BR-03 — set when LOCKED; the lock is released (status -> AVAILABLE) once this passes. */
    @Column(name = "locked_until")
    private Instant lockedUntil;

    /** Identifies which booking session holds the current lock, so only that session can confirm or release it. */
    @Column(name = "lock_token")
    private UUID lockToken;

    @Version
    private long version;

    /** Shared by online (UC-08) and counter (UC-10) sales — a LOCKED seat whose hold has expired is reclaimable. */
    public boolean isAvailableForSale(Instant now) {
        return status == AllocationStatus.AVAILABLE
                || (status == AllocationStatus.LOCKED && lockedUntil != null && lockedUntil.isBefore(now));
    }
}
