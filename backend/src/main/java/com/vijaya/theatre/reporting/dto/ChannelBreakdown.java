package com.vijaya.theatre.reporting.dto;

import java.math.BigDecimal;

public record ChannelBreakdown(int ticketsSold, BigDecimal collections) {
}
