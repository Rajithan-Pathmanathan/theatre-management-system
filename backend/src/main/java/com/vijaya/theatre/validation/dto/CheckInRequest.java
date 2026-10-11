package com.vijaya.theatre.validation.dto;

import jakarta.validation.constraints.Positive;

/** BR-12 — how many tickets this specific entry event admits; a group can arrive in stages. */
public record CheckInRequest(@Positive int ticketsToAdmit) {
}
