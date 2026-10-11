package com.vijaya.theatre.canteen;

import com.vijaya.theatre.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * FR-50–FR-52. {@code itemName}/{@code unitPrice} are snapshotted at order
 * time so a later menu price change never retroactively alters a past
 * order — same reasoning as {@code BookingSeat} not re-deriving prices from
 * the live show.
 */
@Entity
@Table(name = "snack_order_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"snack_order_id", "snack_item_id"}))
@Getter
@Setter
@NoArgsConstructor
public class SnackOrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snack_order_id", nullable = false)
    private SnackOrder snackOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snack_item_id", nullable = false)
    private SnackItem snackItem;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    /** UC-19, FR-64 — staff marks this if the canteen runs out; adjusts the order total. */
    @Column(nullable = false)
    private boolean unavailable = false;

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
