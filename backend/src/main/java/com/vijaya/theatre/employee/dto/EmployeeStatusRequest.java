package com.vijaya.theatre.employee.dto;

/** FR-48 — deactivating also disables any linked staff account (Phase 2 DoD); reactivating does not re-enable it. */
public record EmployeeStatusRequest(boolean active) {
}
