package com.vijaya.theatre.show.dto;

import com.vijaya.theatre.show.SeatZone;

import java.util.UUID;

public record SeatResponse(UUID id, String seatNumber, String rowLabel, int seatInRow, SeatZone zone) {
}
