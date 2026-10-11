package com.vijaya.theatre.canteen;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SnackItemRepository extends JpaRepository<SnackItem, UUID> {

    /** UC-18 — customer ordering only ever offers available items. */
    List<SnackItem> findByAvailableTrue();
}
