package com.vijaya.theatre.show;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShowSeatAllocationRepository extends JpaRepository<ShowSeatAllocation, UUID> {

    List<ShowSeatAllocation> findByShowId(UUID showId);

    // The pessimistic-lock-aware "select seats for update" query belongs to the
    // booking service (next slice), not here — it needs transaction boundaries
    // the repository layer shouldn't own.
}
