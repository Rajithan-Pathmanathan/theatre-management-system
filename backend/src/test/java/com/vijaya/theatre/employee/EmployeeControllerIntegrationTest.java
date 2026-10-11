package com.vijaya.theatre.employee;

import com.vijaya.theatre.AbstractIntegrationTest;
import com.vijaya.theatre.auth.StaffAccount;
import com.vijaya.theatre.auth.StaffAccountRepository;
import com.vijaya.theatre.auth.StaffRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC-16, FR-46–FR-48, BR-09. */
class EmployeeControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private StaffAccountRepository staffAccountRepository;

    @Test
    void adminCanCreateListAndGetAnEmployee() throws Exception {
        String token = loginAsNewAdmin();

        String body = """
                {"fullName":"Kamal Perera","role":"Usher","phoneNumber":"+94771112222","email":"kamal@example.com","linkedStaffAccountId":null}
                """;
        String createResponse = mockMvc.perform(post("/api/admin/employees").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName", is("Kamal Perera")))
                .andExpect(jsonPath("$.role", is("Usher")))
                .andExpect(jsonPath("$.active", is(true)))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(createResponse).get("id").asString();

        mockMvc.perform(get("/api/admin/employees/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("Kamal Perera")));

        mockMvc.perform(get("/api/admin/employees").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + id + "')]").exists());
    }

    @Test
    void adminCanUpdateAnEmployee() throws Exception {
        String token = loginAsNewAdmin();
        String id = createEmployee(token, "Nimal Silva", "Cashier", null);

        String updateBody = """
                {"fullName":"Nimal Silva","role":"Senior Cashier","phoneNumber":"+94779998888","email":null,"linkedStaffAccountId":null}
                """;
        mockMvc.perform(put("/api/admin/employees/" + id).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("Senior Cashier")))
                .andExpect(jsonPath("$.phoneNumber", is("+94779998888")));
    }

    @Test
    void deactivatingAnEmployee_alsoDisablesTheLinkedStaffAccount() throws Exception {
        String token = loginAsNewAdmin();
        StaffAccount linkedAccount = seedStaff("linked-" + UUID.randomUUID(), "a-strong-password", StaffRole.COUNTER_STAFF);
        String id = createEmployee(token, "Door Staff", "Counter Staff", linkedAccount.getId());

        mockMvc.perform(patch("/api/admin/employees/" + id + "/status").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));

        assertThat(staffAccountRepository.findById(linkedAccount.getId()).orElseThrow().isActive()).isFalse();
    }

    @Test
    void reactivatingAnEmployee_doesNotReEnableTheLinkedStaffAccount() throws Exception {
        String token = loginAsNewAdmin();
        StaffAccount linkedAccount = seedStaff("linked2-" + UUID.randomUUID(), "a-strong-password", StaffRole.COUNTER_STAFF);
        String id = createEmployee(token, "Door Staff 2", "Counter Staff", linkedAccount.getId());

        mockMvc.perform(patch("/api/admin/employees/" + id + "/status").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/admin/employees/" + id + "/status").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(true)));

        assertThat(staffAccountRepository.findById(linkedAccount.getId()).orElseThrow().isActive())
                .as("reactivating the employee should not silently re-enable login").isFalse();
    }

    @Test
    void linkingAStaffAccountAlreadyLinkedToAnotherEmployee_isRejected() throws Exception {
        String token = loginAsNewAdmin();
        StaffAccount account = seedStaff("shared-" + UUID.randomUUID(), "a-strong-password", StaffRole.COUNTER_STAFF);
        createEmployee(token, "First Link", "Counter Staff", account.getId());

        String body = """
                {"fullName":"Second Link","role":"Counter Staff","phoneNumber":null,"email":null,"linkedStaffAccountId":"%s"}
                """.formatted(account.getId());
        mockMvc.perform(post("/api/admin/employees").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void linkingANonexistentStaffAccount_returns404() throws Exception {
        String token = loginAsNewAdmin();
        String body = """
                {"fullName":"Ghost Link","role":"Counter Staff","phoneNumber":null,"email":null,"linkedStaffAccountId":"%s"}
                """.formatted(UUID.randomUUID());
        mockMvc.perform(post("/api/admin/employees").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonAdminCannotAccessEmployees() throws Exception {
        String username = "counter-" + UUID.randomUUID();
        seedStaff(username, "a-strong-password", StaffRole.COUNTER_STAFF);
        String token = loginAs(username, "a-strong-password");

        mockMvc.perform(get("/api/admin/employees").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private String createEmployee(String adminToken, String fullName, String role, UUID linkedStaffAccountId) throws Exception {
        String linkedJson = linkedStaffAccountId == null ? "null" : "\"" + linkedStaffAccountId + "\"";
        String body = """
                {"fullName":"%s","role":"%s","phoneNumber":null,"email":null,"linkedStaffAccountId":%s}
                """.formatted(fullName, role, linkedJson);
        String response = mockMvc.perform(post("/api/admin/employees").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asString();
    }
}
