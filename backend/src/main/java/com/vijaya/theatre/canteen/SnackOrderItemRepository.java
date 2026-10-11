package com.vijaya.theatre.canteen;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SnackOrderItemRepository extends JpaRepository<SnackOrderItem, UUID> {

    List<SnackOrderItem> findBySnackOrderId(UUID snackOrderId);
}
