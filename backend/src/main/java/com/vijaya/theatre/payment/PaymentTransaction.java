package com.vijaya.theatre.payment;

import com.vijaya.theatre.booking.Booking;
import com.vijaya.theatre.common.jpa.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * FR-21–FR-23. One row per online booking's payment attempt and outcome.
 * NFR-07 — never stores card/payment-instrument data, only the gateway's own
 * transaction reference and its reported status.
 */
@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
public class PaymentTransaction extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentGateway gateway;

    @Column(name = "gateway_transaction_id")
    private String gatewayTransactionId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "LKR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentStatus status = PaymentStatus.INITIATED;

    /** Raw gateway callback payload, kept for troubleshooting a disputed/failed payment. Never logged elsewhere. */
    @Column(name = "raw_response", columnDefinition = "text")
    private String rawResponse;

    /** When the gateway reported a final SUCCESS/FAILED outcome — distinct from {@code getUpdatedAt()}. */
    @Column(name = "completed_at")
    private Instant completedAt;
}
