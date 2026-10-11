package com.vijaya.theatre.auth.dto;

public record LoginResponse(String token, long expiresInSeconds, StaffProfileResponse staff) {
}
