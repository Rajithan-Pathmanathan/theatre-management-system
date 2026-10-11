package com.vijaya.theatre.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * UC-01's precondition is "the requester is an authenticated administrator"
 * — which means nobody can ever call {@code POST /api/auth/register} until
 * an admin already exists. This breaks that chicken-and-egg problem: on an
 * empty {@code staff_accounts} table, seed exactly one admin from
 * {@code app.admin-seed.*} (env-overridable; the application.yml defaults
 * are for local dev only — see .env.example).
 */
@Component
class AdminAccountSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountSeeder.class);

    private final StaffAccountRepository staffAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedUsername;
    private final String seedPassword;

    AdminAccountSeeder(StaffAccountRepository staffAccountRepository, PasswordEncoder passwordEncoder,
                        @Value("${app.admin-seed.username}") String seedUsername,
                        @Value("${app.admin-seed.password}") String seedPassword) {
        this.staffAccountRepository = staffAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedUsername = seedUsername;
        this.seedPassword = seedPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (staffAccountRepository.count() > 0) {
            return;
        }

        StaffAccount admin = new StaffAccount();
        admin.setUsername(seedUsername);
        admin.setPasswordHash(passwordEncoder.encode(seedPassword));
        admin.setFullName("Theatre Administrator");
        admin.setRole(StaffRole.ADMIN);
        admin.setActive(true);
        staffAccountRepository.save(admin);

        log.warn("No staff accounts existed — seeded initial admin account '{}'. "
                + "Log in and change this password immediately (PUT /api/auth/me/password).", seedUsername);
    }
}
