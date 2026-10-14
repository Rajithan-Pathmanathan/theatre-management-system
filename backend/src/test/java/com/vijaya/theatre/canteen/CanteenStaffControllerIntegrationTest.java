package com.vijaya.theatre.canteen;

import com.vijaya.theatre.AbstractIntegrationTest;
import com.vijaya.theatre.auth.StaffRole;
import com.vijaya.theatre.movie.dto.MovieRequest;
import com.vijaya.theatre.show.dto.ShowRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC-19, BR-16, FR-64. */
class CanteenStaffControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void canteenStaffCanWalkAnOrderThroughToCollected() throws Exception {
        String adminToken = loginAsNewAdmin();
        String canteenToken = loginAsNewCanteenStaff();
        UUID showId = createShow(adminToken);
        String itemId = createSnackItem(adminToken, "Popcorn", "850.00");
        String orderJson = placeOrder(showId, itemId, "PLACED Test Customer", "+94770001111");
        String orderId = objectMapper.readTree(orderJson).get("id").asString();

        mockMvc.perform(patch("/api/canteen/orders/" + orderId + "/status").header("Authorization", "Bearer " + canteenToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"READY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("READY")));

        mockMvc.perform(patch("/api/canteen/orders/" + orderId + "/status").header("Authorization", "Bearer " + canteenToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COLLECTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COLLECTED")));

        mockMvc.perform(patch("/api/canteen/orders/" + orderId + "/status").header("Authorization", "Bearer " + canteenToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"READY\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void staffCanManuallyCloseAnUncollectedOrder_butNeverAutomatically() throws Exception {
        String adminToken = loginAsNewAdmin();
        String canteenToken = loginAsNewCanteenStaff();
        UUID showId = createShow(adminToken);
        String itemId = createSnackItem(adminToken, "Nachos", "650.00");
        String orderJson = placeOrder(showId, itemId, "Uncollected Customer", "+94770002222");
        String orderId = objectMapper.readTree(orderJson).get("id").asString();

        mockMvc.perform(patch("/api/canteen/orders/" + orderId + "/status").header("Authorization", "Bearer " + canteenToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CLOSED")));

        mockMvc.perform(patch("/api/canteen/orders/" + orderId + "/status").header("Authorization", "Bearer " + canteenToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"READY\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void markingAnItemUnavailable_adjustsTheOrderTotal() throws Exception {
        String adminToken = loginAsNewAdmin();
        String canteenToken = loginAsNewCanteenStaff();
        UUID showId = createShow(adminToken);
        String item1 = createSnackItem(adminToken, "Fries", "500.00");
        String item2 = createSnackItem(adminToken, "Coke", "350.00");

        String body = """
                {"showId":"%s","customerName":"Two Item Customer","customerPhone":"+94770003333",
                 "pickupTiming":"BEFORE_MOVIE","items":[{"snackItemId":"%s","quantity":1},{"snackItemId":"%s","quantity":1}]}
                """.formatted(showId, item1, item2);
        String orderResponse = mockMvc.perform(post("/api/snack-orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount", is(850.00)))
                .andReturn().getResponse().getContentAsString();
        String orderId = objectMapper.readTree(orderResponse).get("id").asString();
        String friesItemId = null;
        for (var itemNode : objectMapper.readTree(orderResponse).get("items")) {
            if (itemNode.get("itemName").asString().equals("Fries")) {
                friesItemId = itemNode.get("id").asString();
            }
        }

        mockMvc.perform(patch("/api/canteen/orders/" + orderId + "/items/" + friesItemId + "/availability")
                        .header("Authorization", "Bearer " + canteenToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"unavailable\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount", is(350.00)))
                .andExpect(jsonPath("$.items[?(@.itemName == 'Fries')].unavailable", org.hamcrest.Matchers.contains(true)));
    }

    @Test
    void queueFiltersByDatePickupTimingAndStatus() throws Exception {
        String adminToken = loginAsNewAdmin();
        String canteenToken = loginAsNewCanteenStaff();
        UUID showId = createShow(adminToken);
        String itemId = createSnackItem(adminToken, "Candy", "250.00");
        String orderJson = placeOrder(showId, itemId, "Queue Test Customer", "+94770004444");
        LocalDate orderDate = LocalDate.parse(objectMapper.readTree(orderJson).get("orderDate").asString());

        mockMvc.perform(get("/api/canteen/orders").param("date", orderDate.toString()).param("pickupTiming", "BEFORE_MOVIE")
                        .header("Authorization", "Bearer " + canteenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.customerName == 'Queue Test Customer')]").exists());

        mockMvc.perform(get("/api/canteen/orders").param("date", orderDate.toString()).param("status", "READY")
                        .header("Authorization", "Bearer " + canteenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.customerName == 'Queue Test Customer')]").isEmpty());
    }

    @Test
    void lookupByOrderToken_findsTheOrderForPickupVerification() throws Exception {
        String adminToken = loginAsNewAdmin();
        String canteenToken = loginAsNewCanteenStaff();
        UUID showId = createShow(adminToken);
        String itemId = createSnackItem(adminToken, "Combo", "1200.00");
        String orderJson = placeOrder(showId, itemId, "QR Test Customer", "+94770005555");
        String orderId = objectMapper.readTree(orderJson).get("id").asString();

        // The order token isn't exposed in the API response (same convention as Booking.qrToken) —
        // read it straight from the entity via the autowired repository instead.
        SnackOrder order = snackOrderRepository.findById(UUID.fromString(orderId)).orElseThrow();

        mockMvc.perform(get("/api/canteen/orders/lookup").param("orderToken", order.getOrderToken().toString())
                        .header("Authorization", "Bearer " + canteenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(orderId)));
    }

    @Test
    void counterStaffCannotAccessTheCanteenQueue() throws Exception {
        String username = "counter-" + UUID.randomUUID();
        seedStaff(username, "a-strong-password", StaffRole.COUNTER_STAFF);
        String token = loginAs(username, "a-strong-password");

        mockMvc.perform(get("/api/canteen/orders").param("date", LocalDate.now().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @org.springframework.beans.factory.annotation.Autowired
    private SnackOrderRepository snackOrderRepository;

    private String placeOrder(UUID showId, String itemId, String customerName, String customerPhone) throws Exception {
        String body = """
                {"showId":"%s","customerName":"%s","customerPhone":"%s",
                 "pickupTiming":"BEFORE_MOVIE","items":[{"snackItemId":"%s","quantity":1}]}
                """.formatted(showId, customerName, customerPhone, itemId);
        return mockMvc.perform(post("/api/snack-orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
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
        MovieRequest movieRequest = new MovieRequest("Canteen Fulfil Test Movie " + UUID.randomUUID(), "Tamil", "Drama", 140, null, null);
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
