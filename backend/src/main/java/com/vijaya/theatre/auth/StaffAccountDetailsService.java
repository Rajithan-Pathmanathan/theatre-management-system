package com.vijaya.theatre.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Backs Spring Security's {@code DaoAuthenticationProvider} during login (UC-02) only. */
@Service
class StaffAccountDetailsService implements UserDetailsService {

    private final StaffAccountRepository staffAccountRepository;

    StaffAccountDetailsService(StaffAccountRepository staffAccountRepository) {
        this.staffAccountRepository = staffAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return staffAccountRepository.findByUsername(username)
                .map(StaffPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("No account for username: " + username));
    }
}
