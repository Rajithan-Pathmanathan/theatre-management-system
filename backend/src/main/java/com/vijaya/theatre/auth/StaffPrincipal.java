package com.vijaya.theatre.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Security view of a {@link StaffAccount}, used only during login
 * (authenticated via username/password through {@link StaffAccountDetailsService}).
 * Authorities are the raw {@link StaffRole} name — this codebase uses
 * {@code hasAuthority("ADMIN")} throughout, not {@code hasRole(...)}, so
 * there is deliberately no "ROLE_" prefix here.
 */
record StaffPrincipal(UUID staffId, String username, String passwordHash, StaffRole role, boolean active)
        implements UserDetails {

    static StaffPrincipal from(StaffAccount account) {
        return new StaffPrincipal(account.getId(), account.getUsername(), account.getPasswordHash(),
                account.getRole(), account.isActive());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    /** UC-02 alternative flow A2 — a deactivated account is refused at authentication time, not after. */
    @Override
    public boolean isEnabled() {
        return active;
    }
}
