package com.vijaya.theatre.canteen.dto;

import java.math.BigDecimal;

public record CanteenItemSales(String itemName, int quantitySold, BigDecimal sales) {
}
