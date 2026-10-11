package com.vijaya.theatre.auth.dto;

import com.vijaya.theatre.auth.StaffRole;

import java.util.UUID;

public record StaffProfileResponse(
        UUID id,
        String username,
        String fullName,
        String email,
        String phoneNumber,
        StaffRole role,
        boolean active
) {
}
