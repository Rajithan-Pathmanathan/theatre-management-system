package com.vijaya.theatre.reporting;

import com.vijaya.theatre.reporting.dto.DailyReportResponse;
import com.vijaya.theatre.reporting.dto.ShowReportResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/** UC-14. ADMIN only — gated by the {@code /api/admin/**} rule in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/reports")
class ReportingController {

    private final ReportingService reportingService;

    ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/daily")
    DailyReportResponse daily(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reportingService.getDailyReport(date);
    }

    @GetMapping("/shows/{showId}")
    ShowReportResponse show(@PathVariable UUID showId) {
        return reportingService.getShowReport(showId);
    }
}
