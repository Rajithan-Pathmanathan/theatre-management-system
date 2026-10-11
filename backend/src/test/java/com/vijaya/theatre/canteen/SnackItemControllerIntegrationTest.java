package com.vijaya.theatre.canteen;

import com.vijaya.theatre.AbstractIntegrationTest;
import com.vijaya.theatre.auth.StaffRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC-17, FR-49. */
class SnackItemControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void adminCanCreateAndUpdateASnackItem() throws Exception {
        String token = loginAsNewAdmin();

        String createResponse = mockMvc.perform(post("/api/admin/snack-items").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Popcorn (Large)\",\"price\":850.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Popcorn (Large)")))
                .andExpect(jsonPath("$.available", is(true)))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(createResponse).get("id").asString();

        mockMvc.perform(put("/api/admin/snack-items/" + id).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Popcorn (Large)\",\"price\":900.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price", is(900.00)));
    }

    @Test
    void togglingAvailability_removesAnItemFromThePublicListingButNotTheAdminOne() throws Exception {
        String token = loginAsNewAdmin();
        String createResponse = mockMvc.perform(post("/api/admin/snack-items").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Nachos\",\"price\":650.00}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(createResponse).get("id").asString();

        mockMvc.perform(get("/api/snacks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + id + "')]").exists());

        mockMvc.perform(patch("/api/admin/snack-items/" + id + "/availability").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"available\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available", is(false)));

        mockMvc.perform(get("/api/snacks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + id + "')]").isEmpty());

        mockMvc.perform(get("/api/admin/snack-items").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + id + "')]").exists());
    }

    @Test
    void publicSnackListing_needsNoAuthentication() throws Exception {
        mockMvc.perform(get("/api/snacks")).andExpect(status().isOk());
    }

    @Test
    void nonAdminCannotManageSnackItems() throws Exception {
        String username = "counter-" + UUID.randomUUID();
        seedStaff(username, "a-strong-password", StaffRole.COUNTER_STAFF);
        String token = loginAs(username, "a-strong-password");

        mockMvc.perform(post("/api/admin/snack-items").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Hack\",\"price\":1.00}"))
                .andExpect(status().isForbidden());
    }
}
