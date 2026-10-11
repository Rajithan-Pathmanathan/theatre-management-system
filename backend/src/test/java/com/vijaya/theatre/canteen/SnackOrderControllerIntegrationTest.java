package com.vijaya.theatre.canteen;

import com.vijaya.theatre.AbstractIntegrationTest;
import com.vijaya.theatre.movie.dto.MovieRequest;
import com.vijaya.theatre.show.AllocationChannel;
import com.vijaya.theatre.show.ShowSeatAllocation;
import com.vijaya.theatre.show.ShowSeatAllocationRepository;
import com.vijaya.theatre.show.dto.ShowRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC-18, BR-08, BR-13–BR-15. */
class SnackOrderControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ShowSeatAllocationRepository showSeatAllocationRepository;

    @Test
    void standaloneOrder_needsNoBookingAndGetsASequentialOrderNumber() throws Exception {
        String adminToken = loginAsNewAdmin();
        UUID showId = createShow(adminToken, true);
        String itemId = createSnackItem(adminToken, "Popcorn", "850.00");

        String body = """
                {"showId":"%s","customerName":"Walk-in Customer","customerPhone":"+94770001111",
                 "pickupTiming":"BEFORE_MOVIE","items":[{"snackItemId":"%s","quantity":2}]}
                """.formatted(showId, itemId);

        mockMvc.perform(post("/api/snack-orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").isNumber())
                .andExpect(jsonPath("$.status", is("PLACED")))
                .andExpect(jsonPath("$.totalAmount", is(1700.00)))
                .andExpect(jsonPath("$.items.length()", is(1)))
                .andExpect(jsonPath("$.linkedBookingReference").doesNotExist());
    }

    @Test
    void secondOrderTheSameDay_getsTheNextSequentialNumber() throws Exception {
        String adminToken = loginAsNewAdmin();
        UUID showId = createShow(adminToken, true);
        String itemId = createSnackItem(adminToken, "Nachos", "650.00");

        String body = """
                {"showId":"%s","customerName":"Test","customerPhone":"+94770001111",
                 "pickupTiming":"BEFORE_MOVIE","items":[{"snackItemId":"%s","quantity":1}]}
                """.formatted(showId, itemId);

        String first = mockMvc.perform(post("/api/snack-orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(post("/api/snack-orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        int firstNumber = objectMapper.readTree(first).get("orderNumber").asInt();
        int secondNumber = objectMapper.readTree(second).get("orderNumber").asInt();
        assertThat(secondNumber).isEqualTo(firstNumber + 1);
    }

    @Test
    void bookingLinkedOrder_derivesItsShowFromTheBookingAndRecordsTheReference() throws Exception {
        String adminToken = loginAsNewAdmin();
        UUID showId = createShow(adminToken, true);
        String itemId = createSnackItem(adminToken, "Coke", "350.00");

        UUID onlineSeatId = showSeatAllocationRepository.findByShowId(showId).stream()
                .filter(a -> a.getChannel() == AllocationChannel.ONLINE).findFirst().orElseThrow().getId();
        String bookingBody = """
                {"showId":"%s","adultCount":1,"childCount":0,"seatAllocationIds":["%s"],
                 "customerName":"Nimal","customerPhone":"+94771234567","customerEmail":null}
                """.formatted(showId, onlineSeatId);
        String bookingResponse = mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(bookingBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String bookingId = objectMapper.readTree(bookingResponse).get("id").asString();
        String bookingRef = objectMapper.readTree(bookingResponse).get("bookingReference").asString();

        String orderBody = """
                {"bookingId":"%s","customerName":"Nimal","customerPhone":"+94771234567",
                 "pickupTiming":"DURING_INTERVAL","items":[{"snackItemId":"%s","quantity":1}]}
                """.formatted(bookingId, itemId);

        mockMvc.perform(post("/api/snack-orders").contentType(MediaType.APPLICATION_JSON).content(orderBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.showId", is(showId.toString())))
                .andExpect(jsonPath("$.linkedBookingReference", is(bookingRef)));
    }

    @Test
    void orderingForAShowWithNoInterval_rejectsDuringIntervalPickup() throws Exception {
        String adminToken = loginAsNewAdmin();
        UUID showId = createShow(adminToken, false);
        String itemId = createSnackItem(adminToken, "Fries", "500.00");

        String body = """
                {"showId":"%s","customerName":"Test","customerPhone":"+94770001111",
                 "pickupTiming":"DURING_INTERVAL","items":[{"snackItemId":"%s","quantity":1}]}
                """.formatted(showId, itemId);

        mockMvc.perform(post("/api/snack-orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void orderingAnUnavailableItem_isRejected() throws Exception {
        String adminToken = loginAsNewAdmin();
        UUID showId = createShow(adminToken, true);
        String itemId = createSnackItem(adminToken, "Hot Dog", "700.00");
        mockMvc.perform(patch("/api/admin/snack-items/" + itemId + "/availability")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"available\":false}"))
                .andExpect(status().isOk());

        String body = """
                {"showId":"%s","customerName":"Test","customerPhone":"+94770001111",
                 "pickupTiming":"BEFORE_MOVIE","items":[{"snackItemId":"%s","quantity":1}]}
                """.formatted(showId, itemId);

        mockMvc.perform(post("/api/snack-orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void neitherBookingNorShowProvided_isRejected() throws Exception {
        String adminToken = loginAsNewAdmin();
        String itemId = createSnackItem(adminToken, "Candy", "250.00");

        String body = """
                {"customerName":"Test","customerPhone":"+94770001111",
                 "pickupTiming":"BEFORE_MOVIE","items":[{"snackItemId":"%s","quantity":1}]}
                """.formatted(itemId);

        mockMvc.perform(post("/api/snack-orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void lookupByOrderNumber_findsTheSameOrder_andQrCodeIsAValidPng() throws Exception {
        String adminToken = loginAsNewAdmin();
        UUID showId = createShow(adminToken, true);
        String itemId = createSnackItem(adminToken, "Popcorn Combo", "1200.00");

        String body = """
                {"showId":"%s","customerName":"Lookup Test","customerPhone":"+94770009999",
                 "pickupTiming":"BEFORE_MOVIE","items":[{"snackItemId":"%s","quantity":1}]}
                """.formatted(showId, itemId);
        String createResponse = mockMvc.perform(post("/api/snack-orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(createResponse).get("id").asString();
        int orderNumber = objectMapper.readTree(createResponse).get("orderNumber").asInt();
        String orderDate = objectMapper.readTree(createResponse).get("orderDate").asString();

        mockMvc.perform(get("/api/snack-orders/lookup").param("date", orderDate).param("orderNumber", String.valueOf(orderNumber)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id)));

        byte[] png = mockMvc.perform(get("/api/snack-orders/" + id + "/qr-code"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(png.length).isGreaterThan(0);
        assertThat(png[0]).isEqualTo((byte) 0x89); // PNG magic byte
    }

    private String createSnackItem(String adminToken, String name, String price) throws Exception {
        String response = mockMvc.perform(post("/api/admin/snack-items").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"price\":" + price + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asString();
    }

    private UUID createShow(String adminToken, boolean hasInterval) throws Exception {
        MovieRequest movieRequest = new MovieRequest("Canteen Test Movie " + UUID.randomUUID(), "Tamil", "Drama", 140, null, null);
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
                new BigDecimal("450.00"), new BigDecimal("300.00"), true, true, 40, hasInterval);
        String showResponse = mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(showResponse).get("id").asString());
    }
}
