package com.vijaya.theatre.booking;

import com.vijaya.theatre.common.jpa.BaseEntity;
import com.vijaya.theatre.show.ShowSeatAllocation;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * FR-16, FR-26 — the specific seats attached to a booking. Correctness
 * against double-booking (FR-12) is enforced where the seat is actually
 * contended: {@link ShowSeatAllocation#getStatus()} under a pessimistic lock,
 * not a constraint on this join table.
 */
@Entity
@Table(name = "booking_seats",
        uniqueConstraints = @UniqueConstraint(columnNames = {"booking_id", "show_seat_allocation_id"}))
@Getter
@Setter
@NoArgsConstructor
public class BookingSeat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_seat_allocation_id", nullable = false)
    private ShowSeatAllocation showSeatAllocation;
}
