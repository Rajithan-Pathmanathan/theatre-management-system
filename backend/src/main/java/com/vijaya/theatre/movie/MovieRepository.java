package com.vijaya.theatre.movie;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MovieRepository extends JpaRepository<Movie, UUID> {

    /** UC-07 — customer browsing excludes INACTIVE movies only (SHOWING and UPCOMING are both browsable). */
    List<Movie> findByStatusNot(MovieStatus status);
}
