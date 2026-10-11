package com.vijaya.theatre.show;

import com.vijaya.theatre.common.jpa.AuditableEntity;
import com.vijaya.theatre.movie.Movie;
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
import java.time.LocalTime;

/**
 * FR-04–FR-08, UC-05. A single screening of a {@link Movie}. Online booking is
 * only possible when both {@code guaranteed} and {@code onlineBookingEnabled}
 * are true (BR-01) — the two are kept separate because a guaranteed show can
 * still have online sales paused independently (UC-05 alternative flow A5).
 */
@Entity
@Table(name = "shows")
@Getter
@Setter
@NoArgsConstructor
public class Show extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(name = "show_date", nullable = false)
    private LocalDate showDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShowStatus status = ShowStatus.SCHEDULED;

    @Column(name = "adult_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal adultPrice;

    @Column(name = "child_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal childPrice;

    /** BR-01 — only guaranteed shows may ever be opened for online booking. */
    @Column(nullable = false)
    private boolean guaranteed = false;

    /** Independent of {@code guaranteed} — lets admin pause/resume online sales for a guaranteed show. */
    @Column(name = "online_booking_enabled", nullable = false)
    private boolean onlineBookingEnabled = false;

    /** BR-05 — % of hall capacity released for online booking; the rest is physical. 0–100. */
    @Column(name = "online_allocation_percent", nullable = false)
    private int onlineAllocationPercent = 0;

    /** BR-14 (Phase 3) — drives whether "During Interval" canteen pickup is offered for this show. */
    @Column(name = "has_interval", nullable = false)
    private boolean hasInterval = true;

    /** Shared by online (UC-08) and counter (UC-10) sales — both price against this show's own rates. */
    public BigDecimal calculateTotal(int adultCount, int childCount) {
        return adultPrice.multiply(BigDecimal.valueOf(adultCount))
                .add(childPrice.multiply(BigDecimal.valueOf(childCount)));
    }
}
