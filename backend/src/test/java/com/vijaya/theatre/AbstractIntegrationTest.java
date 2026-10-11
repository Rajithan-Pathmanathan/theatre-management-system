package com.vijaya.theatre;

import com.vijaya.theatre.auth.StaffAccount;
import com.vijaya.theatre.auth.StaffAccountRepository;
import com.vijaya.theatre.auth.StaffRole;
import com.vijaya.theatre.auth.dto.LoginRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared scaffolding for every controller-level integration test: a real
 * MockMvc against a real Testcontainers Postgres, plus helpers for getting
 * an authenticated staff token without re-deriving the login flow in every
 * test class. Used by every integration test added after
 * AuthControllerIntegrationTest (left as-is rather than retrofitted, to
 * avoid churning already-passing, already-committed test code).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected StaffAccountRepository staffAccountRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected StaffAccount seedStaff(String username, String rawPassword, StaffRole role) {
        StaffAccount account = new StaffAccount();
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(rawPassword));
        account.setFullName("Test " + username);
        account.setRole(role);
        account.setActive(true);
        return staffAccountRepository.save(account);
    }

    protected String loginAs(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = objectMapper.readValue(response, Map.class);
        return (String) parsed.get("token");
    }

    protected String loginAsNewAdmin() throws Exception {
        String username = "admin-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        seedStaff(username, "a-strong-password", StaffRole.ADMIN);
        return loginAs(username, "a-strong-password");
    }
}
