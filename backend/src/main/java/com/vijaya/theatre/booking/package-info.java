/**
 * Online seat booking, seat locking and booking lifecycle management
 * (UC-08, UC-09 booking half, UC-12, UC-13; FR-15–FR-20, FR-24–FR-27;
 * BR-02, BR-03, BR-04, BR-11, BR-12).
 * <p>Phase 1. Owns {@code Booking}, {@code BookingSeat} and {@code SeatLock} —
 * the highest-risk module in the project (concurrent seat selection, NFR-02).</p>
 */
package com.vijaya.theatre.booking;
