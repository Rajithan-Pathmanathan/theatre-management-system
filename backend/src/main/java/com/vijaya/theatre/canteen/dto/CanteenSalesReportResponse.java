package com.vijaya.theatre.canteen.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** FR-57. */
public record CanteenSalesReportResponse(LocalDate date, int ordersCollected, BigDecimal totalSales, List<CanteenItemSales> items) {
}
