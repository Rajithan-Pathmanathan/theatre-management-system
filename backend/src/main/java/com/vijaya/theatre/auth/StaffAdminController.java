package com.vijaya.theatre.auth;

import com.vijaya.theatre.auth.dto.StaffProfileResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** UC-16 support — ADMIN only, gated by the {@code /api/admin/**} rule in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/staff")
class StaffAdminController {

    private final AuthService authService;

    StaffAdminController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    List<StaffProfileResponse> list() {
        return authService.list();
    }
}
