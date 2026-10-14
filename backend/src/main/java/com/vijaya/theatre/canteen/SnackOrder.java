package com.vijaya.theatre.canteen;

import com.vijaya.theatre.booking.Booking;
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
import java.time.LocalDate;
import java.util.UUID;

/**
 * UC-18, UC-19; FR-50–FR-65; BR-08, BR-13–BR-16. {@code linkedBooking} is
 * optional and purely contextual (which ticket booking this customer also
 * has, for the canteen queue's benefit) — BR-08 means an order never
 * requires one, and this module never reaches into the booking/payment
 * core's own machinery for it.
 * <p>BR-13: snacks added on a booking's payment page start as
 * AWAITING_PAYMENT and are charged with the tickets through the payment
 * module's {@code BookingAddOns} port ({@link CanteenBookingAddOns}), so
 * payment never depends on this module. Every other order is
 * pay-at-collection ({@code prepaidAmount} null).</p>
 */
@Entity
@Table(name = "snack_orders")
@Getter
@Setter
@NoArgsConstructor
public class SnackOrder extends AuditableEntity {

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    /** BR-15 — sequential and customer-facing; unique per {@code orderDate}, not globally. */
    @Column(name = "order_number", nullable = false)
    private int orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_booking_id")
    private Booking linkedBooking;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "pickup_timing", nullable = false, length = 20)
    private PickupTiming pickupTiming;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SnackOrderStatus status = SnackOrderStatus.PLACED;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /** UC-19 — lets staff verify pickup via a scanned QR, the same pattern as Booking.qrToken. */
    @Column(name = "order_token", nullable = false, unique = true)
    private UUID orderToken;

    /** BR-13 — what was charged online with the booking; null means pay at collection. */
    @Column(name = "prepaid_amount", precision = 10, scale = 2)
    private BigDecimal prepaidAmount;
}
