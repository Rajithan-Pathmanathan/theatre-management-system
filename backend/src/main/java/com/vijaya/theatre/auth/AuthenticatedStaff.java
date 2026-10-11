package com.vijaya.theatre.auth;

import java.util.UUID;

/**
 * The authenticated principal for every JWT-bearing request — set by
 * {@link JwtAuthenticationFilter} directly from the token's claims. Inject
 * this into a controller method via {@code @AuthenticationPrincipal} to find
 * out who is calling without a database lookup.
 */
public record AuthenticatedStaff(UUID staffId, String username, StaffRole role) {
}
