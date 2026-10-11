package com.vijaya.theatre.auth;

import com.vijaya.theatre.auth.dto.ChangePasswordRequest;
import com.vijaya.theatre.auth.dto.LoginRequest;
import com.vijaya.theatre.auth.dto.LoginResponse;
import com.vijaya.theatre.auth.dto.RegisterRequest;
import com.vijaya.theatre.auth.dto.StaffProfileResponse;
import com.vijaya.theatre.auth.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** UC-01, UC-02, UC-03. Authorization for /register is path-based (SecurityConfig, ADMIN only). */
@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final AuthService authService;

    AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    ResponseEntity<StaffProfileResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    StaffProfileResponse getOwnProfile(@AuthenticationPrincipal AuthenticatedStaff principal) {
        return authService.getProfile(principal.staffId());
    }

    @PutMapping("/me")
    StaffProfileResponse updateOwnProfile(@AuthenticationPrincipal AuthenticatedStaff principal,
                                           @Valid @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(principal.staffId(), request);
    }

    @PutMapping("/me/password")
    ResponseEntity<Void> changeOwnPassword(@AuthenticationPrincipal AuthenticatedStaff principal,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.staffId(), request);
        return ResponseEntity.noContent().build();
    }
}
