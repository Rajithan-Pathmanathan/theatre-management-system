package com.vijaya.theatre.canteen;

import com.vijaya.theatre.AbstractIntegrationTest;
import com.vijaya.theatre.auth.StaffRole;
import com.vijaya.theatre.movie.dto.MovieRequest;
import com.vijaya.theatre.show.dto.ShowRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FR-57. */
class CanteenReportControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void dailyReport_countsOnlyCollectedOrdersAndAvailableItems() throws Exception {
        String adminToken = loginAsNewAdmin();
        String canteenToken = loginAsNewCanteenStaff();
        UUID showId = createShow(adminToken);
        String popcorn = "Report Popcorn " + UUID.randomUUID();
        String coke = "Report Coke " + UUID.randomUUID();
        String popcornId = createSnackItem(adminToken, popcorn, "800.00");
        String cokeId = createSnackItem(adminToken, coke, "300.00");

        // Order A: 2 popcorn + 1 coke; the coke runs out, then the order is collected.
        JsonNode orderA = placeOrder(showId, popcornId, 2, cokeId);
        String date = orderA.get("orderDate").asString();
        JsonNode before = report(adminToken, date);
        for (JsonNode item : orderA.get("items")) {
            if (item.get("itemName").asString().equals(coke)) {
                mockMvc.perform(patch("/api/canteen/orders/" + orderA.get("id").asString() + "/items/" + item.get("id").asString() + "/availability")
                                .header("Authorization", "Bearer " + canteenToken)
                                .contentType(MediaType.APPLICATION_JSON).content("{\"unavailable\":true}"))
                        .andExpect(status().isOk());
            }
        }
        collect(canteenToken, orderA.get("id").asString());

        // Order B: 1 popcorn, collected. Order C: 1 popcorn, never collected.
        collect(canteenToken, placeOrder(showId, popcornId, 1, null).get("id").asString());
        placeOrder(showId, popcornId, 1, null);

        // Other tests may have orders on the same date, so compare deltas for the totals.
        JsonNode after = report(adminToken, date);
        assertThat(after.get("ordersCollected").asInt() - before.get("ordersCollected").asInt()).isEqualTo(2);
        assertThat(after.get("totalSales").decimalValue().subtract(before.get("totalSales").decimalValue()))
                .isEqualByComparingTo("2400.00");

        mockMvc.perform(get("/api/admin/canteen/reports/daily").param("date", date).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.itemName == '" + popcorn + "')].quantitySold", contains(3)))
                .andExpect(jsonPath("$.items[?(@.itemName == '" + popcorn + "')].sales", contains(2400.00)))
                .andExpect(jsonPath("$.items[?(@.itemName == '" + coke + "')]", empty()));
    }

    @Test
    void canteenStaffCannotSeeTheSalesReport() throws Exception {
        mockMvc.perform(get("/api/admin/canteen/reports/daily").param("date", LocalDate.now().toString())
                        .header("Authorization", "Bearer " + loginAsNewCanteenStaff()))
                .andExpect(status().isForbidden());
    }

    private JsonNode report(String adminToken, String date) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get("/api/admin/canteen/reports/daily").param("date", date)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private void collect(String canteenToken, String orderId) throws Exception {
        mockMvc.perform(patch("/api/canteen/orders/" + orderId + "/status").header("Authorization", "Bearer " + canteenToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COLLECTED\"}"))
                .andExpect(status().isOk());
    }

    private JsonNode placeOrder(UUID showId, String itemId, int quantity, String extraItemId) throws Exception {
        String items = "{\"snackItemId\":\"" + itemId + "\",\"quantity\":" + quantity + "}"
                + (extraItemId == null ? "" : ",{\"snackItemId\":\"" + extraItemId + "\",\"quantity\":1}");
        String body = """
                {"showId":"%s","customerName":"Report Customer","customerPhone":"+94770006666",
                 "pickupTiming":"BEFORE_MOVIE","items":[%s]}
                """.formatted(showId, items);
        return objectMapper.readTree(mockMvc.perform(post("/api/snack-orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private String loginAsNewCanteenStaff() throws Exception {
        String username = "canteen-" + UUID.randomUUID().toString().substring(0, 8);
        seedStaff(username, "a-strong-password", StaffRole.CANTEEN_STAFF);
        return loginAs(username, "a-strong-password");
    }

    private String createSnackItem(String adminToken, String name, String price) throws Exception {
        String response = mockMvc.perform(post("/api/admin/snack-items").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"price\":" + price + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asString();
    }

    private UUID createShow(String adminToken) throws Exception {
        MovieRequest movieRequest = new MovieRequest("Canteen Report Test Movie " + UUID.randomUUID(), "Tamil", "Drama", 140, null, null);
        String movieResponse = mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movieRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID movieId = UUID.fromString(objectMapper.readTree(movieResponse).get("id").asString());

        int dayOffset = ThreadLocalRandom.current().nextInt(24, 300);
        int hour = ThreadLocalRandom.current().nextInt(10, 23);
        ShowRequest showRequest = new ShowRequest(movieId, LocalDate.now().plusDays(dayOffset), LocalTime.of(hour, 0),
                new BigDecimal("450.00"), new BigDecimal("300.00"), true, true, 40, true);
        String showResponse = mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(showResponse).get("id").asString());
    }
}
