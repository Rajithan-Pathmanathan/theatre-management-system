package com.vijaya.theatre.canteen;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SnackOrderRepository extends JpaRepository<SnackOrder, UUID> {

    /** UC-18 — the human-facing lookup (order number + the day it was placed). */
    Optional<SnackOrder> findByOrderDateAndOrderNumber(LocalDate orderDate, int orderNumber);

    /** UC-19 — pickup verification via a scanned QR. */
    Optional<SnackOrder> findByOrderToken(UUID orderToken);

    /** BR-15 — the next sequential number for a day is this order's number + 1 (wrapping at 300). */
    Optional<SnackOrder> findTopByOrderDateOrderByOrderNumberDesc(LocalDate orderDate);

    /** UC-19 — the canteen staff queue for a given day. */
    List<SnackOrder> findByOrderDate(LocalDate orderDate);
}
