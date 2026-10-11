package com.vijaya.theatre.show;

import com.vijaya.theatre.AbstractIntegrationTest;
import com.vijaya.theatre.auth.StaffRole;
import com.vijaya.theatre.movie.dto.MovieRequest;
import com.vijaya.theatre.show.dto.SeatAllocationUpdateRequest;
import com.vijaya.theatre.show.dto.ShowRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UC-06. Covers the two genuinely risky pieces: the distribution algorithm
 * (BR-06 — spread across zones, not one clump) and the "frozen seat" rule
 * (FR-11/AC-04 — reconfiguring never touches a BOOKED or LOCKED seat).
 */
class SeatAllocationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ShowSeatAllocationRepository showSeatAllocationRepository;

    @Test
    void creatingAShow_initializes300AllocationsMatchingItsOnlineAllocationPercent() throws Exception {
        String token = loginAsNewAdmin();
        UUID showId = createShow(token, 40);

        List<ShowSeatAllocation> allocations = showSeatAllocationRepository.findByShowId(showId);
        assertThat(allocations).hasSize(300);

        long online = allocations.stream().filter(a -> a.getChannel() == AllocationChannel.ONLINE).count();
        // 40% of 300 is 120; independent per-zone rounding can land within a seat or two of that.
        assertThat(online).isBetween(115L, 125L);
    }

    @Test
    void onlineSeats_areSpreadAcrossMoreThanOneZone_notConcentratedInOne() throws Exception {
        String token = loginAsNewAdmin();
        UUID showId = createShow(token, 40);

        var onlineZones = showSeatAllocationRepository.findByShowId(showId).stream()
                .filter(a -> a.getChannel() == AllocationChannel.ONLINE)
                .map(a -> a.getSeat().getZone())
                .collect(Collectors.toSet());

        assertThat(onlineZones).as("online seats must land in more than one zone").hasSizeGreaterThan(1);

        // AC-03 in full: no single zone is entirely online or entirely physical.
        var byZone = showSeatAllocationRepository.findByShowId(showId).stream()
                .collect(Collectors.groupingBy(a -> a.getSeat().getZone()));
        byZone.forEach((zone, zoneAllocations) -> {
            long zoneOnline = zoneAllocations.stream().filter(a -> a.getChannel() == AllocationChannel.ONLINE).count();
            assertThat(zoneOnline).as(zone + " must be a mix, not all-online or all-physical")
                    .isGreaterThan(0).isLessThan(zoneAllocations.size());
        });
    }

    @Test
    void reconfiguring_changesTheDistributionAndPersistsTheNewPercent() throws Exception {
        String token = loginAsNewAdmin();
        UUID showId = createShow(token, 20);

        long onlineBefore = showSeatAllocationRepository.findByShowId(showId).stream()
                .filter(a -> a.getChannel() == AllocationChannel.ONLINE).count();

        mockMvc.perform(put("/api/admin/shows/" + showId + "/seat-allocation")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SeatAllocationUpdateRequest(80))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onlineAllocationPercent", is(80)));

        long onlineAfter = showSeatAllocationRepository.findByShowId(showId).stream()
                .filter(a -> a.getChannel() == AllocationChannel.ONLINE).count();
        assertThat(onlineAfter).isGreaterThan(onlineBefore);
    }

    @Test
    void reconfiguring_neverTouchesABookedOrLockedSeat() throws Exception {
        String token = loginAsNewAdmin();
        UUID showId = createShow(token, 40);

        // Simulate a real booking having claimed one PHYSICAL seat — nothing can create this via the
        // API yet (no booking slice), so set it up directly through the repository.
        ShowSeatAllocation claimed = showSeatAllocationRepository.findByShowId(showId).stream()
                .filter(a -> a.getChannel() == AllocationChannel.PHYSICAL)
                .findFirst().orElseThrow();
        claimed.setStatus(AllocationStatus.BOOKED);
        showSeatAllocationRepository.save(claimed);
        UUID claimedSeatId = claimed.getSeat().getId();

        // Push the online share way up — if the frozen-seat rule didn't hold, this seat would very
        // likely flip to ONLINE along with most of its zone.
        mockMvc.perform(put("/api/admin/shows/" + showId + "/seat-allocation")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SeatAllocationUpdateRequest(95))))
                .andExpect(status().isOk());

        ShowSeatAllocation reloaded = showSeatAllocationRepository.findByShowId(showId).stream()
                .filter(a -> a.getSeat().getId().equals(claimedSeatId)).findFirst().orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(AllocationStatus.BOOKED);
        assertThat(reloaded.getChannel()).as("a booked seat's channel must never change").isEqualTo(AllocationChannel.PHYSICAL);
    }

    @Test
    void viewingAllocation_returnsAllThreeHundredSeatsWithCounts() throws Exception {
        String token = loginAsNewAdmin();
        UUID showId = createShow(token, 40);

        mockMvc.perform(get("/api/admin/shows/" + showId + "/seat-allocation").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSeats", is(300)))
                .andExpect(jsonPath("$.seats.length()", is(300)));
    }

    @Test
    void nonAdminCannotReconfigureAllocation() throws Exception {
        String adminToken = loginAsNewAdmin();
        UUID showId = createShow(adminToken, 40);
        seedStaff("counter-seat-test", "a-strong-password", StaffRole.COUNTER_STAFF);
        String nonAdminToken = loginAs("counter-seat-test", "a-strong-password");

        mockMvc.perform(put("/api/admin/shows/" + showId + "/seat-allocation")
                        .header("Authorization", "Bearer " + nonAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SeatAllocationUpdateRequest(50))))
                .andExpect(status().isForbidden());
    }

    private UUID createShow(String adminToken, int onlineAllocationPercent) throws Exception {
        MovieRequest movieRequest = new MovieRequest("Seat Test Movie " + UUID.randomUUID(), "Tamil", "Drama", 140, null, null);
        String movieResponse = mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movieRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID movieId = UUID.fromString(objectMapper.readTree(movieResponse).get("id").asString());

        ShowRequest showRequest = new ShowRequest(movieId, LocalDate.now().plusDays(14), LocalTime.of(18, 30),
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
