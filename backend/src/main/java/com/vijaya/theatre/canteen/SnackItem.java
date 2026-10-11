package com.vijaya.theatre.canteen;

import com.vijaya.theatre.common.jpa.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** UC-17, FR-49. Never deleted, only toggled unavailable — an order already placed still references it by snapshot. */
@Entity
@Table(name = "snack_items")
@Getter
@Setter
@NoArgsConstructor
public class SnackItem extends AuditableEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean available = true;
}
