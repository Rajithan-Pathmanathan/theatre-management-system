package com.vijaya.theatre.counter;

import com.vijaya.theatre.AbstractIntegrationTest;
import com.vijaya.theatre.auth.StaffRole;
import com.vijaya.theatre.booking.BookingChannel;
import com.vijaya.theatre.booking.BookingStatus;
import com.vijaya.theatre.movie.dto.MovieRequest;
import com.vijaya.theatre.show.AllocationChannel;
import com.vijaya.theatre.show.AllocationStatus;
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

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC-10, FR-66–FR-68, BR-07, BR-17. */
class CounterSaleControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ShowSeatAllocationRepository showSeatAllocationRepository;

    @Test
    void counterStaffCanSellAPhysicalTicket_confirmedImmediatelyWithNoLockWindow() throws Exception {
        String adminToken = loginAsNewAdmin();
        UUID showId = createShow(adminToken, 40); // 60% physical, plenty of PHYSICAL seats
        String counterToken = loginAsNewCounterStaff();
        List<UUID> seatIds = physicalSeatIds(showId, 2);

        String body = """
                {"showId":"%s","adultCount":1,"childCount":1,"seatAllocationIds":["%s","%s"],
                 "physicalPaymentMethod":"CASH","customerName":"Nimal","customerPhone":"+94770001111"}
                """.formatted(showId, seatIds.get(0), seatIds.get(1));

        mockMvc.perform(post("/api/counter/sales").header("Authorization", "Bearer " + counterToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.totalAmount", is(750.00)))
                .andExpect(jsonPath("$.seats.length()", is(2)))
                .andExpect(jsonPath("$.expiresAt").doesNotExist());

        List<ShowSeatAllocation> sold = showSeatAllocationRepository.findAllById(seatIds);
        sold.forEach(a -> {
            org.assertj.core.api.Assertions.assertThat(a.getStatus()).isEqualTo(AllocationStatus.BOOKED);
            org.assertj.core.api.Assertions.assertThat(a.getLockedUntil()).isNull();
        });
    }

    @Test
    void adminCanAlsoSellACounterTicket() throws Exception {
        String adminToken = loginAsNewAdmin();
        UUID showId = createShow(adminToken, 40);
        List<UUID> seatIds = physicalSeatIds(showId, 1);

        String body = """
                {"showId":"%s","adultCount":1,"childCount":0,"seatAllocationIds":["%s"],
                 "physicalPaymentMethod":"CARD"}
                """.formatted(showId, seatIds.get(0));

        mockMvc.perform(post("/api/counter/sales").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));
    }

    @Test
    void canteenStaffCannotSellACounterTicket() throws Exception {
        String adminToken = loginAsNewAdmin();
        UUID showId = createShow(adminToken, 40);
        List<UUID> seatIds = physicalSeatIds(showId, 1);
        String canteenToken = loginAs(seedStaff("canteen-" + UUID.randomUUID(), "a-strong-password", StaffRole.CANTEEN_STAFF).getUsername(), "a-strong-password");

        String body = """
                {"showId":"%s","adultCount":1,"childCount":0,"seatAllocationIds":["%s"],
                 "physicalPaymentMethod":"CASH"}
                """.formatted(showId, seatIds.get(0));

        mockMvc.perform(post("/api/counter/sales").header("Authorization", "Bearer " + canteenToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void sellingAnOnlineChannelSeat_isRejected() throws Exception {
        String adminToken = loginAsNewAdmin();
        UUID showId = createShow(adminToken, 40);
        String counterToken = loginAsNewCounterStaff();
        UUID onlineSeatId = showSeatAllocationRepository.findByShowId(showId).stream()
                .filter(a -> a.getChannel() == AllocationChannel.ONLINE).findFirst().orElseThrow().getId();

        String body = """
                {"showId":"%s","adultCount":1,"childCount":0,"seatAllocationIds":["%s"],
                 "physicalPaymentMethod":"CASH"}
                """.formatted(showId, onlineSeatId);

        mockMvc.perform(post("/api/counter/sales").header("Authorization", "Bearer " + counterToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void ticketCountNotMatchingSeatCount_violatesBR04() throws Exception {
        String adminToken = loginAsNewAdmin();
        UUID showId = createShow(adminToken, 40);
        String counterToken = loginAsNewCounterStaff();
        List<UUID> seatIds = physicalSeatIds(showId, 1);

        String body = """
                {"showId":"%s","adultCount":1,"childCount":1,"seatAllocationIds":["%s"],
                 "physicalPaymentMethod":"CASH"}
                """.formatted(showId, seatIds.get(0));

        mockMvc.perform(post("/api/counter/sales").header("Authorization", "Bearer " + counterToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void anAlreadyBookedPhysicalSeat_cannotBeSoldAgain() throws Exception {
        String adminToken = loginAsNewAdmin();
        UUID showId = createShow(adminToken, 40);
        String counterToken = loginAsNewCounterStaff();
        UUID seatId = physicalSeatIds(showId, 1).get(0);

        ShowSeatAllocation allocation = showSeatAllocationRepository.findById(seatId).orElseThrow();
        allocation.setStatus(AllocationStatus.BOOKED);
        showSeatAllocationRepository.save(allocation);

        String body = """
                {"showId":"%s","adultCount":1,"childCount":0,"seatAllocationIds":["%s"],
                 "physicalPaymentMethod":"CASH"}
                """.formatted(showId, seatId);

        mockMvc.perform(post("/api/counter/sales").header("Authorization", "Bearer " + counterToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void counterSeatView_onlyEverListsPhysicalChannelSeats() throws Exception {
        String adminToken = loginAsNewAdmin();
        UUID showId = createShow(adminToken, 40);
        String counterToken = loginAsNewCounterStaff();

        long physicalCount = showSeatAllocationRepository.findByShowId(showId).stream()
                .filter(a -> a.getChannel() == AllocationChannel.PHYSICAL)
                .count();

        mockMvc.perform(get("/api/counter/shows/" + showId + "/seats").header("Authorization", "Bearer " + counterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is((int) physicalCount)));
    }

    private String loginAsNewCounterStaff() throws Exception {
        String username = "counter-" + UUID.randomUUID().toString().substring(0, 8);
        seedStaff(username, "a-strong-password", StaffRole.COUNTER_STAFF);
        return loginAs(username, "a-strong-password");
    }

    private List<UUID> physicalSeatIds(UUID showId, int count) {
        return showSeatAllocationRepository.findByShowId(showId).stream()
                .filter(a -> a.getChannel() == AllocationChannel.PHYSICAL)
                .limit(count)
                .map(ShowSeatAllocation::getId)
                .toList();
    }

    private UUID createShow(String adminToken, int onlineAllocationPercent) throws Exception {
        MovieRequest movieRequest = new MovieRequest("Counter Test Movie " + UUID.randomUUID(), "Tamil", "Drama", 140, null, null);
        String movieResponse = mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movieRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID movieId = UUID.fromString(objectMapper.readTree(movieResponse).get("id").asString());

        ShowRequest showRequest = new ShowRequest(movieId, LocalDate.now().plusDays(23), LocalTime.of(20, 15),
                new BigDecimal("450.00"), new BigDecimal("300.00"), true, true, onlineAllocationPercent, true);
        String showResponse = mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(showResponse).get("id").asString());
    }
}
