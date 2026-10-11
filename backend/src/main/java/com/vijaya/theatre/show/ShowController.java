package com.vijaya.theatre.show;

import com.vijaya.theatre.show.dto.ShowRequest;
import com.vijaya.theatre.show.dto.ShowResponse;
import com.vijaya.theatre.show.dto.ShowStatusRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** UC-05. ADMIN only — gated by the {@code /api/admin/**} rule in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/shows")
class ShowController {

    private final ShowService showService;

    ShowController(ShowService showService) {
        this.showService = showService;
    }

    @GetMapping
    List<ShowResponse> listByDate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return showService.listByDate(date);
    }

    @GetMapping("/{id}")
    ShowResponse get(@PathVariable UUID id) {
        return showService.get(id);
    }

    @PostMapping
    ResponseEntity<ShowResponse> create(@Valid @RequestBody ShowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(showService.create(request));
    }

    @PutMapping("/{id}")
    ShowResponse update(@PathVariable UUID id, @Valid @RequestBody ShowRequest request) {
        return showService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    ShowResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody ShowStatusRequest request) {
        return showService.updateStatus(id, request);
    }
}
