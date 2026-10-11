package com.vijaya.theatre.show.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** UC-06, FR-10, BR-05. */
public record SeatAllocationUpdateRequest(@Min(0) @Max(100) int onlineAllocationPercent) {
}
