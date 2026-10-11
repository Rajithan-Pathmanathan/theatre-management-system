/**
 * Staff/admin authentication and account management (UC-01, UC-02, UC-03;
 * governed by NFR-04–NFR-08).
 * <p>Phase 1. Customers never get an account — booking is guest checkout
 * (UC-08). Owns {@code StaffAccount}, Spring Security config and the JWT
 * issuing/validation filter.</p>
 */
package com.vijaya.theatre.auth;
