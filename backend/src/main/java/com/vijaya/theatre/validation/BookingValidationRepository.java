package com.vijaya.theatre.validation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingValidationRepository extends JpaRepository<BookingValidation, UUID> {

    List<BookingValidation> findByBookingId(UUID bookingId);
}
