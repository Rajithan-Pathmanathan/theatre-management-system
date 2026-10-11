package com.vijaya.theatre.employee.dto;

import java.util.UUID;

public record EmployeeResponse(
        UUID id,
        String fullName,
        String role,
        String phoneNumber,
        String email,
        boolean active,
        UUID linkedStaffAccountId,
        String linkedStaffAccountUsername
) {
}
