package com.vijaya.theatre.auth;

import com.vijaya.theatre.TestcontainersConfiguration;
import com.vijaya.theatre.auth.dto.ChangePasswordRequest;
import com.vijaya.theatre.auth.dto.LoginRequest;
import com.vijaya.theatre.auth.dto.RegisterRequest;
import com.vijaya.theatre.auth.dto.UpdateProfileRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises UC-01/UC-02/UC-03 end to end over real HTTP: the seeded admin
 * logs in, registers a counter-staff account, that account logs in and can
 * manage its own profile but is forbidden from the admin-only registration
 * endpoint. This is the highest-risk slice so far after seat locking — every
 * other controller's security depends on this working correctly.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private StaffAccountRepository staffAccountRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.admin-seed.username}")
    private String seedAdminUsername;
    @Value("${app.admin-seed.password}")
    private String seedAdminPassword;

    @Test
    void adminSeederHasAlreadyCreatedOneAdminAccount() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(seedAdminUsername, seedAdminPassword))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.staff.role", is("ADMIN")))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginWithWrongPassword_isRejectedWithoutDisclosingWhichFieldWasWrong() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(seedAdminUsername, "not-the-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid username or password.")));
    }

    @Test
    void adminCanRegisterNewStaff_andThatStaffCanThenLogIn() throws Exception {
        String adminToken = loginAs(seedAdminUsername, seedAdminPassword);

        RegisterRequest newCounterStaff = new RegisterRequest(
                "counter1", "a-strong-password", "Kamal Perera", "kamal@example.com", "+94770000000", StaffRole.COUNTER_STAFF);

        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCounterStaff)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("counter1")))
                .andExpect(jsonPath("$.role", is("COUNTER_STAFF")));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("counter1", "a-strong-password"))))
                .andExpect(status().isOk());
    }

    @Test
    void registeringWithoutAdminRole_isForbidden() throws Exception {
        seedStaff("canteen1", "whatever-password", StaffRole.CANTEEN_STAFF);
        String nonAdminToken = loginAs("canteen1", "whatever-password");

        RegisterRequest attempt = new RegisterRequest(
                "sneaky", "a-strong-password", "Someone", null, null, StaffRole.ADMIN);

        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + nonAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attempt)))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpointWithoutAToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownProfile_canBeViewedAndUpdated_butNotItsRole() throws Exception {
        StaffAccount account = seedStaff("counter2", "a-strong-password", StaffRole.COUNTER_STAFF);
        String token = loginAs("counter2", "a-strong-password");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("counter2")))
                .andExpect(jsonPath("$.role", is("COUNTER_STAFF")));

        UpdateProfileRequest update = new UpdateProfileRequest("Updated Name", "updated@example.com", "+94771111111");
        mockMvc.perform(put("/api/auth/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("Updated Name")))
                // role is untouched — UpdateProfileRequest has no role field to begin with (A3)
                .andExpect(jsonPath("$.role", is("COUNTER_STAFF")));

        StaffAccount reloaded = staffAccountRepository.findById(account.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(reloaded.getFullName()).isEqualTo("Updated Name");
    }

    @Test
    void changingPassword_requiresTheCorrectCurrentPassword() throws Exception {
        seedStaff("counter3", "original-password", StaffRole.COUNTER_STAFF);
        String token = loginAs("counter3", "original-password");

        mockMvc.perform(put("/api/auth/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangePasswordRequest("wrong-current", "new-password-123"))))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/auth/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangePasswordRequest("original-password", "new-password-123"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("counter3", "new-password-123"))))
                .andExpect(status().isOk());
    }

    private StaffAccount seedStaff(String username, String rawPassword, StaffRole role) {
        StaffAccount account = new StaffAccount();
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(rawPassword));
        account.setFullName("Test " + username);
        account.setRole(role);
        account.setActive(true);
        return staffAccountRepository.save(account);
    }

    private String loginAs(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = objectMapper.readValue(response, Map.class);
        return (String) parsed.get("token");
    }
}
