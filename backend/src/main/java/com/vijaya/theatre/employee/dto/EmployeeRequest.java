package com.vijaya.theatre.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/** UC-16 — FR-46 (create) and FR-47 (update) share this shape; status changes go through {@link EmployeeStatusRequest}. */
public record EmployeeRequest(
        @NotBlank String fullName,
        @NotBlank String role,
        String phoneNumber,
        @Email String email,
        UUID linkedStaffAccountId
) {
}
