package com.vijaya.theatre.validation;

import com.vijaya.theatre.auth.StaffAccount;
import com.vijaya.theatre.booking.Booking;
import com.vijaya.theatre.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * FR-30–FR-32, BR-12, NFR-21. One row per check-in event at the theatre door.
 * A booking can have several of these (partial entry — a group arriving in
 * stages); {@code Booking.ticketsUsed} is the running total, this table is
 * the audit trail behind it.
 */
@Entity
@Table(name = "booking_validations")
@Getter
@Setter
@NoArgsConstructor
public class BookingValidation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "validated_by_staff_id", nullable = false)
    private StaffAccount validatedBy;

    /** How many tickets this specific entry event admitted (not the booking's running total). */
    @Column(name = "tickets_validated", nullable = false)
    private int ticketsValidated;

    @Column(name = "validated_at", nullable = false)
    private Instant validatedAt = Instant.now();
}
