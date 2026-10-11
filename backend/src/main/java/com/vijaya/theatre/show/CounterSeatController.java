package com.vijaya.theatre.show;

import com.vijaya.theatre.show.dto.PublicSeatResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** UC-10. COUNTER_STAFF/ADMIN only — gated in SecurityConfig; lives here for SeatAllocationService/Mapper access. */
@RestController
@RequestMapping("/api/counter/shows/{showId}/seats")
class CounterSeatController {

    private final SeatAllocationService seatAllocationService;

    CounterSeatController(SeatAllocationService seatAllocationService) {
        this.seatAllocationService = seatAllocationService;
    }

    @GetMapping
    List<PublicSeatResponse> getPhysicalSeats(@PathVariable UUID showId) {
        return seatAllocationService.getPhysicalSeats(showId);
    }
}
