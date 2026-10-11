package com.vijaya.theatre.show;

import com.vijaya.theatre.show.dto.PublicSeatResponse;
import com.vijaya.theatre.show.dto.PublicShowResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** UC-07. Public — gated as permitAll in SecurityConfig, not under /api/admin. */
@RestController
@RequestMapping("/api/shows")
class ShowPublicController {

    private final ShowService showService;

    ShowPublicController(ShowService showService) {
        this.showService = showService;
    }

    @GetMapping
    List<PublicShowResponse> listByDate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return showService.listPublicByDate(date);
    }

    @GetMapping("/{id}")
    PublicShowResponse get(@PathVariable UUID id) {
        return showService.getPublicShow(id);
    }

    @GetMapping("/{id}/seats")
    List<PublicSeatResponse> getSeats(@PathVariable UUID id) {
        return showService.getPublicSeats(id);
    }
}
