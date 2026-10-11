package com.vijaya.theatre.booking;

import com.vijaya.theatre.auth.StaffAccount;
import com.vijaya.theatre.common.jpa.AuditableEntity;
import com.vijaya.theatre.show.Show;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * FR-13–FR-32, BR-02–BR-04, BR-07, BR-11, BR-12, BR-17. One row per booking,
 * online or counter (BR-07 — both channels share this table so occupancy and
 * reporting stay unified). There is no separate Customer entity: customers
 * never hold an account (guest checkout, UC-08), so their contact details are
 * just fields here rather than a row in another table.
 */
@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
public class Booking extends AuditableEntity {

    /** Human-presentable identifier, e.g. "VJY-20261011-7F3K2A" — what the customer and staff actually use (BR-11). */
    @Column(name = "booking_reference", nullable = false, unique = true, length = 30)
    private String bookingReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BookingChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "adult_count", nullable = false)
    private int adultCount;

    @Column(name = "child_count", nullable = false)
    private int childCount;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /** FR-25, BR-11 — one QR per booking regardless of seat count; encodes to/decodes from this token. Set on confirmation. */
    @Column(name = "qr_token", unique = true)
    private UUID qrToken;

    /** BR-12 — running count of tickets checked in so far; remaining = (adultCount + childCount) - ticketsUsed. */
    @Column(name = "tickets_used", nullable = false)
    private int ticketsUsed = 0;

    /** BR-17 — counter sales only; null for online bookings, which carry a {@code PaymentTransaction} instead. */
    @Enumerated(EnumType.STRING)
    @Column(name = "physical_payment_method", length = 10)
    private PhysicalPaymentMethod physicalPaymentMethod;

    /** Counter sales only — which staff member sold it. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sold_by_staff_id")
    private StaffAccount soldByStaff;

    /** Online bookings only — when the PENDING_PAYMENT seat lock expires (BR-03); null once CONFIRMED. */
    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    public int remainingTickets() {
        return adultCount + childCount - ticketsUsed;
    }

    public boolean isFullyCheckedIn() {
        return remainingTickets() <= 0;
    }
}
