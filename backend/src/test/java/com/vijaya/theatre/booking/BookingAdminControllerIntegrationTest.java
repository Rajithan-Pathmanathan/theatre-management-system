package com.vijaya.theatre.booking;

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

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC-13. */
class BookingAdminControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ShowSeatAllocationRepository showSeatAllocationRepository;

    @Test
    void adminCanListBookingsFilteredByShowAndChannel() throws Exception {
        String adminToken = loginAsNewAdmin();
        UUID showId = createShow(adminToken);
        String bookingRef = sellCounterTicket(adminToken, showId, "+94771110001");

        mockMvc.perform(get("/api/admin/bookings").param("showId", showId.toString()).param("channel", "COUNTER")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].bookingReference", is(bookingRef)))
                .andExpect(jsonPath("$[0].channel", is("COUNTER")))
                .andExpect(jsonPath("$[0].soldByStaffUsername").isNotEmpty());
    }

    @Test
    void adminCanFilterByReference() throws Exception {
        String adminToken = loginAsNewAdmin();
        UUID showId = createShow(adminToken);
        String bookingRef = sellCounterTicket(adminToken, showId, "+94771110002");

        mockMvc.perform(get("/api/admin/bookings").param("reference", bookingRef).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].bookingReference", is(bookingRef)));

        mockMvc.perform(get("/api/admin/bookings").param("reference", "VJY-NOTAREALREFERENCE").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    void adminCanGetASingleBookingDetail() throws Exception {
        String adminToken = loginAsNewAdmin();
        UUID showId = createShow(adminToken);
        String bookingRef = sellCounterTicket(adminToken, showId, "+94771110003");
        UUID bookingId = UUID.fromString(objectMapper.readTree(
                        mockMvc.perform(get("/api/admin/bookings").param("reference", bookingRef).header("Authorization", "Bearer " + adminToken))
                                .andReturn().getResponse().getContentAsString())
                .get(0).get("id").asString());

        mockMvc.perform(get("/api/admin/bookings/" + bookingId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingReference", is(bookingRef)))
                .andExpect(jsonPath("$.physicalPaymentMethod", is("CASH")))
                .andExpect(jsonPath("$.seats.length()", is(1)));
    }

    @Test
    void nonAdminCannotListBookings() throws Exception {
        String username = "counter-" + UUID.randomUUID().toString().substring(0, 8);
        seedStaff(username, "a-strong-password", com.vijaya.theatre.auth.StaffRole.COUNTER_STAFF);
        String counterToken = loginAs(username, "a-strong-password");

        mockMvc.perform(get("/api/admin/bookings").header("Authorization", "Bearer " + counterToken))
                .andExpect(status().isForbidden());
    }

    private String sellCounterTicket(String adminToken, UUID showId, String phone) throws Exception {
        UUID seatId = showSeatAllocationRepository.findByShowId(showId).stream()
                .filter(a -> a.getChannel() == AllocationChannel.PHYSICAL)
                .map(ShowSeatAllocation::getId)
                .findFirst().orElseThrow();

        String body = """
                {"showId":"%s","adultCount":1,"childCount":0,"seatAllocationIds":["%s"],
                 "physicalPaymentMethod":"CASH","customerName":"Admin List Test","customerPhone":"%s"}
                """.formatted(showId, seatId, phone);

        String response = mockMvc.perform(post("/api/counter/sales").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("bookingReference").asString();
    }

    private UUID createShow(String adminToken) throws Exception {
        MovieRequest movieRequest = new MovieRequest("Admin Booking Test Movie " + UUID.randomUUID(), "Tamil", "Drama", 140, null, null);
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
