package com.vijaya.theatre.canteen;

import com.vijaya.theatre.canteen.dto.CanteenSalesReportResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** FR-57. ADMIN only — gated by the {@code /api/admin/**} rule in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/canteen/reports")
class CanteenReportController {

    private final SnackOrderService snackOrderService;

    CanteenReportController(SnackOrderService snackOrderService) {
        this.snackOrderService = snackOrderService;
    }

    @GetMapping("/daily")
    CanteenSalesReportResponse daily(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return snackOrderService.getDailySalesReport(date);
    }
}
