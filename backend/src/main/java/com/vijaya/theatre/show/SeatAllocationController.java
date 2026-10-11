package com.vijaya.theatre.show;

import com.vijaya.theatre.show.dto.SeatAllocationSummaryResponse;
import com.vijaya.theatre.show.dto.SeatAllocationUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** UC-06. ADMIN only — gated by the {@code /api/admin/**} rule in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/shows/{showId}/seat-allocation")
class SeatAllocationController {

    private final SeatAllocationService seatAllocationService;

    SeatAllocationController(SeatAllocationService seatAllocationService) {
        this.seatAllocationService = seatAllocationService;
    }

    @GetMapping
    SeatAllocationSummaryResponse get(@PathVariable UUID showId) {
        return seatAllocationService.getSummary(showId);
    }

    @PutMapping
    SeatAllocationSummaryResponse update(@PathVariable UUID showId, @Valid @RequestBody SeatAllocationUpdateRequest request) {
        return seatAllocationService.reconfigureAndSummarize(showId, request.onlineAllocationPercent());
    }
}
