package com.vijaya.theatre.reporting;

import com.vijaya.theatre.reporting.dto.DailyReportResponse;
import com.vijaya.theatre.reporting.dto.ShowReportResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    private static final MediaType CSV = MediaType.parseMediaType("text/csv");

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

    /** FR-48 (Phase 2 export path). */
    @GetMapping(value = "/daily/export", produces = "text/csv")
    ResponseEntity<String> exportDaily(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"daily-report-" + date + ".csv\"")
                .contentType(CSV)
                .body(reportingService.exportDailyReportCsv(date));
    }

    /** FR-48 (Phase 2 export path). */
    @GetMapping(value = "/shows/{showId}/export", produces = "text/csv")
    ResponseEntity<String> exportShow(@PathVariable UUID showId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"show-report-" + showId + ".csv\"")
                .contentType(CSV)
                .body(reportingService.exportShowReportCsv(showId));
    }
}
