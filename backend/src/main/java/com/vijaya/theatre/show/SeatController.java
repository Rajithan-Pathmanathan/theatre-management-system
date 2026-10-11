package com.vijaya.theatre.show;

import com.vijaya.theatre.show.dto.SeatResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/**
 * FR-09. Read-only — the hall layout is seed data (V2__seed_seat_layout.sql),
 * not an admin CRUD workflow; see that migration's header comment for why.
 */
@RestController
@RequestMapping("/api/admin/seats")
class SeatController {

    private final SeatRepository seatRepository;
    private final SeatMapper seatMapper;

    SeatController(SeatRepository seatRepository, SeatMapper seatMapper) {
        this.seatRepository = seatRepository;
        this.seatMapper = seatMapper;
    }

    @GetMapping
    List<SeatResponse> list() {
        return seatRepository.findAll().stream()
                .sorted(Comparator.comparing(Seat::getRowLabel).thenComparing(Seat::getSeatInRow))
                .map(seatMapper::toResponse)
                .toList();
    }
}
