package com.vijaya.theatre.show;

import com.vijaya.theatre.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * FR-09. The theatre's fixed ~300-seat ODC layout — master data, independent
 * of any particular show. Per-show availability/channel lives on
 * {@link ShowSeatAllocation}, one row per (show, seat).
 */
@Entity
@Table(name = "seats")
@Getter
@Setter
@NoArgsConstructor
public class Seat extends BaseEntity {

    /** Customer-facing label, e.g. "C12". */
    @Column(name = "seat_number", nullable = false, unique = true, length = 10)
    private String seatNumber;

    @Column(name = "row_label", nullable = false, length = 5)
    private String rowLabel;

    @Column(name = "seat_in_row", nullable = false)
    private Integer seatInRow;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SeatZone zone;
}
