package com.vijaya.theatre.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** UC-03 — deliberately has no {@code role} field: a staff member can never change their own role (alternative flow A3). */
public record UpdateProfileRequest(
        @NotBlank String fullName,
        @Email String email,
        String phoneNumber
) {
}
