package com.vijaya.theatre.auth;

import com.vijaya.theatre.auth.dto.ChangePasswordRequest;
import com.vijaya.theatre.auth.dto.LoginRequest;
import com.vijaya.theatre.auth.dto.LoginResponse;
import com.vijaya.theatre.auth.dto.RegisterRequest;
import com.vijaya.theatre.auth.dto.StaffProfileResponse;
import com.vijaya.theatre.auth.dto.UpdateProfileRequest;
import com.vijaya.theatre.common.exception.BusinessRuleViolationException;
import com.vijaya.theatre.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** UC-01, UC-02, UC-03. */
@Service
@Transactional
class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final StaffAccountRepository staffAccountRepository;
    private final StaffAccountMapper staffAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    AuthService(StaffAccountRepository staffAccountRepository, StaffAccountMapper staffAccountMapper,
                PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.staffAccountRepository = staffAccountRepository;
        this.staffAccountMapper = staffAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /** UC-01. Caller is already authorized as ADMIN by {@code SecurityConfig} before this runs. */
    StaffProfileResponse register(RegisterRequest request) {
        if (staffAccountRepository.existsByUsername(request.username())) {
            // Alternative flow A1 — reject rather than silently overwrite an existing account.
            throw new BusinessRuleViolationException("An account already exists for username '" + request.username() + "'.");
        }

        StaffAccount account = new StaffAccount();
        account.setUsername(request.username());
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setFullName(request.fullName());
        account.setEmail(request.email());
        account.setPhoneNumber(request.phoneNumber());
        account.setRole(request.role());
        account.setActive(true);

        account = staffAccountRepository.save(account);
        log.info("Registered new {} account '{}'", account.getRole(), account.getUsername());
        return staffAccountMapper.toProfileResponse(account);
    }

    /**
     * UC-02. Delegates wrong-password vs unknown-username vs deactivated-account
     * distinctions to Spring Security's {@code AuthenticationManager}, which
     * deliberately reports all of them as one generic failure (alternative flow
     * A1) — handled uniformly by {@code GlobalExceptionHandler}.
     * <p>Deliberately not implemented here: the five-failed-attempts temporary
     * lockout from UC-02 alternative flow A1. Not exercised by any Phase 1
     * acceptance criterion; add failed_attempts/locked_until columns on
     * StaffAccount if the client asks for it.
     */
    LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        StaffAccount account = staffAccountRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalStateException("Authenticated but account vanished: " + request.username()));

        String token = jwtService.issueToken(account);
        log.info("Staff '{}' logged in", account.getUsername());
        return new LoginResponse(token, jwtService.expirationSeconds(), staffAccountMapper.toProfileResponse(account));
    }

    /** UC-03 (view). */
    StaffProfileResponse getProfile(UUID staffId) {
        return staffAccountMapper.toProfileResponse(requireAccount(staffId));
    }

    /** UC-03 (update) — contact details only; role is never editable here (alternative flow A3). */
    StaffProfileResponse updateProfile(UUID staffId, UpdateProfileRequest request) {
        StaffAccount account = requireAccount(staffId);
        account.setFullName(request.fullName());
        account.setEmail(request.email());
        account.setPhoneNumber(request.phoneNumber());
        return staffAccountMapper.toProfileResponse(staffAccountRepository.save(account));
    }

    /** UC-03 (password change) — alternative flow A2: current password must verify first. */
    void changePassword(UUID staffId, ChangePasswordRequest request) {
        StaffAccount account = requireAccount(staffId);
        if (!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw new BusinessRuleViolationException("Current password is incorrect.");
        }
        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        staffAccountRepository.save(account);
        log.info("Staff '{}' changed their password", account.getUsername());
    }

    private StaffAccount requireAccount(UUID staffId) {
        return staffAccountRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff account not found: " + staffId));
    }
}
