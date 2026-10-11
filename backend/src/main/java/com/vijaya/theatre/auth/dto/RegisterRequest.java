package com.vijaya.theatre.auth.dto;

import com.vijaya.theatre.auth.StaffRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** UC-01 — submitted by an already-authenticated admin to create a new staff/admin account. */
public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank String fullName,
        @Email String email,
        String phoneNumber,
        @NotNull StaffRole role
) {
}
