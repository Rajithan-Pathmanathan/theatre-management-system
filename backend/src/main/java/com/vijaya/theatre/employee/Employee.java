package com.vijaya.theatre.employee;

import com.vijaya.theatre.auth.StaffAccount;
import com.vijaya.theatre.common.jpa.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * UC-16, FR-46–FR-48, BR-09. Deliberately basic — name, role, contact,
 * status only; no attendance, shifts, or payroll (out of scope per the SRS).
 * {@code linkedStaffAccount} is optional: not every employee has a system
 * login (e.g. ushers), and not every login belongs to a recorded employee
 * (e.g. the seeded admin account) — see {@code StaffAccount}'s own Javadoc
 * for why the two were never coupled at the auth level.
 */
@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
public class Employee extends AuditableEntity {

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String role;

    @Column(name = "phone_number")
    private String phoneNumber;

    private String email;

    @Column(nullable = false)
    private boolean active = true;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_staff_account_id")
    private StaffAccount linkedStaffAccount;
}
