package com.vijaya.theatre.show.dto;

import com.vijaya.theatre.show.ShowStatus;
import jakarta.validation.constraints.NotNull;

/**
 * FR-08. Setting CANCELLED here only changes the show's own status — UC-05
 * alternative flow A4's "flag existing bookings for refund and notify
 * customers" isn't implemented yet, since no booking can exist until the
 * booking slice is built. Revisit this when UC-13 (Manage Bookings) lands.
 */
public record ShowStatusRequest(@NotNull ShowStatus status) {
}
