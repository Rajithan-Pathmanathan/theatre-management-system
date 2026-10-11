package com.vijaya.theatre.show;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ShowRepository extends JpaRepository<Show, UUID> {

    List<Show> findByShowDate(LocalDate showDate);

    /** UC-05 alternative flow A1 — candidates to check for a time-slot overlap on a given date. */
    List<Show> findByShowDateAndStatus(LocalDate showDate, ShowStatus status);

    /** UC-04 alternative flow A4 — blocks deactivating a movie that still has shows scheduled against it. */
    boolean existsByMovieIdAndStatusAndShowDateGreaterThanEqual(UUID movieId, ShowStatus status, LocalDate fromDate);

    /** UC-15 — all shows a movie has ever had, for its cross-show performance analytics. */
    List<Show> findByMovieId(UUID movieId);

    /** UC-15 — the occupancy trend over a date range. */
    List<Show> findByShowDateBetween(LocalDate from, LocalDate to);
}
