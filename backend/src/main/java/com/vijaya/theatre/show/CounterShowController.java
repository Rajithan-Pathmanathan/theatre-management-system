package com.vijaya.theatre.show;

import com.vijaya.theatre.show.dto.PublicSeatResponse;
import com.vijaya.theatre.show.dto.ShowResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * UC-10. COUNTER_STAFF/ADMIN only — gated in SecurityConfig; lives here (not
 * in the {@code counter} package) for ShowService/SeatAllocationService
 * access. Unlike {@code ShowPublicController}, show listing here isn't
 * filtered to guaranteed/online-enabled — a counter sale is valid for any
 * SCHEDULED show regardless of its online-booking flags.
 */
@RestController
@RequestMapping("/api/counter/shows")
class CounterShowController {

    private final ShowService showService;
    private final SeatAllocationService seatAllocationService;

    CounterShowController(ShowService showService, SeatAllocationService seatAllocationService) {
        this.showService = showService;
        this.seatAllocationService = seatAllocationService;
    }

    @GetMapping
    List<ShowResponse> listByDate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return showService.listByDate(date);
    }

    @GetMapping("/{showId}/seats")
    List<PublicSeatResponse> getPhysicalSeats(@PathVariable UUID showId) {
        return seatAllocationService.getPhysicalSeats(showId);
    }
}
