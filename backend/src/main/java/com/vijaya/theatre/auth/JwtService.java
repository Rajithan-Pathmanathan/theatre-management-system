package com.vijaya.theatre.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues and validates the staff/admin JWT (UC-01/UC-02, governed by
 * NFR-04–NFR-08). The token itself carries the username, staff id and role
 * as claims, so a request is authenticated without a database round trip —
 * the trade-off is that a role change or deactivation only takes effect once
 * the token expires (see {@code app.jwt.expiration-minutes}), which is the
 * standard, accepted trade-off for stateless JWT auth at this scale.
 */
@Component
class JwtService {

    private static final String CLAIM_STAFF_ID = "staffId";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey signingKey;
    private final long expirationMinutes;

    JwtService(@Value("${app.jwt.secret}") String secret,
               @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    String issueToken(StaffAccount account) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(account.getUsername())
                .claim(CLAIM_STAFF_ID, account.getId().toString())
                .claim(CLAIM_ROLE, account.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(signingKey)
                .compact();
    }

    long expirationSeconds() {
        return expirationMinutes * 60;
    }

    /** Empty if the token is missing, malformed, expired, or has a bad signature — caller treats all of those alike. */
    Optional<AuthenticatedClaims> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new AuthenticatedClaims(
                    claims.getSubject(),
                    UUID.fromString(claims.get(CLAIM_STAFF_ID, String.class)),
                    StaffRole.valueOf(claims.get(CLAIM_ROLE, String.class))
            ));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    record AuthenticatedClaims(String username, UUID staffId, StaffRole role) {
    }
}
