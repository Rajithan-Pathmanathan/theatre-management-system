package com.vijaya.theatre.auth;

import com.vijaya.theatre.common.jpa.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * UC-01/UC-02/UC-03. Staff/admin accounts only — customers never get one
 * (guest checkout, UC-08). Carries its own identity fields rather than a
 * foreign key to an Employee record: Employee management (UC-16, BR-09) is
 * Phase 2, built after auth, so gating account creation on it now would
 * invert the plan's own phase order. Revisit linking the two once Employee
 * exists, if the client actually wants that coupling.
 */
@Entity
@Table(name = "staff_accounts")
@Getter
@Setter
@NoArgsConstructor
public class StaffAccount extends AuditableEntity {

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StaffRole role;

    @Column(nullable = false)
    private boolean active = true;
}
