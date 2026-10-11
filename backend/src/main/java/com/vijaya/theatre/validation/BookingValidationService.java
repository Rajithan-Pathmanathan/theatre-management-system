package com.vijaya.theatre.validation;

import com.vijaya.theatre.auth.StaffAccount;
import com.vijaya.theatre.auth.StaffAccountRepository;
import com.vijaya.theatre.booking.Booking;
import com.vijaya.theatre.booking.BookingRepository;
import com.vijaya.theatre.booking.BookingSeatRepository;
import com.vijaya.theatre.booking.BookingStatus;
import com.vijaya.theatre.booking.dto.BookedSeatResponse;
import com.vijaya.theatre.common.exception.BusinessRuleViolationException;
import com.vijaya.theatre.common.exception.ResourceNotFoundException;
import com.vijaya.theatre.show.Show;
import com.vijaya.theatre.validation.dto.BookingLookupResponse;
import com.vijaya.theatre.validation.dto.CheckInRequest;
import com.vijaya.theatre.validation.dto.CheckInResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * UC-11, FR-28–FR-32, BR-11, BR-12. Door-side check-in: find a booking by
 * reference, QR token, or phone number, then admit some or all of its
 * remaining tickets. {@link Booking#remainingTickets()} already enforces
 * that a fully checked-in booking can't be over-admitted; this service is
 * what writes the audit trail ({@link BookingValidation}) behind that count.
 */
@Service
@Transactional
class BookingValidationService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingValidationRepository bookingValidationRepository;
    private final StaffAccountRepository staffAccountRepository;

    BookingValidationService(BookingRepository bookingRepository, BookingSeatRepository bookingSeatRepository,
                              BookingValidationRepository bookingValidationRepository,
                              StaffAccountRepository staffAccountRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.bookingValidationRepository = bookingValidationRepository;
        this.staffAccountRepository = staffAccountRepository;
    }

    /** Exactly one of reference/qrToken/phone must be given; phone can match several bookings. */
    @Transactional(readOnly = true)
    List<BookingLookupResponse> lookup(String reference, UUID qrToken, String phone) {
        int provided = (reference != null ? 1 : 0) + (qrToken != null ? 1 : 0) + (phone != null ? 1 : 0);
        if (provided != 1) {
            throw new BusinessRuleViolationException("Provide exactly one of reference, qrToken, or phone.");
        }

        List<Booking> bookings;
        if (reference != null) {
            bookings = bookingRepository.findByBookingReference(reference).map(List::of).orElseGet(List::of);
        } else if (qrToken != null) {
            bookings = bookingRepository.findByQrToken(qrToken).map(List::of).orElseGet(List::of);
        } else {
            bookings = bookingRepository.findByCustomerPhone(phone);
        }
        return bookings.stream().map(this::toLookupResponse).toList();
    }

    CheckInResponse checkIn(UUID staffId, UUID bookingId, CheckInRequest request) {
        Booking booking = requireBooking(bookingId);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessRuleViolationException("Only a confirmed booking can be validated.");
        }
        if (booking.isFullyCheckedIn()) {
            throw new BusinessRuleViolationException("This booking has already been fully checked in.");
        }
        if (request.ticketsToAdmit() > booking.remainingTickets()) {
            throw new BusinessRuleViolationException(
                    "Cannot admit " + request.ticketsToAdmit() + " ticket(s) — only " + booking.remainingTickets() + " remain.");
        }

        StaffAccount staff = staffAccountRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff account not found: " + staffId));

        BookingValidation validation = new BookingValidation();
        validation.setBooking(booking);
        validation.setValidatedBy(staff);
        validation.setTicketsValidated(request.ticketsToAdmit());
        validation.setValidatedAt(Instant.now());
        bookingValidationRepository.save(validation);

        booking.setTicketsUsed(booking.getTicketsUsed() + request.ticketsToAdmit());
        bookingRepository.save(booking);

        return new CheckInResponse(booking.getId(), booking.getBookingReference(), request.ticketsToAdmit(),
                booking.getTicketsUsed(), booking.remainingTickets(), booking.isFullyCheckedIn(), validation.getValidatedAt());
    }

    private BookingLookupResponse toLookupResponse(Booking booking) {
        List<BookedSeatResponse> seats = bookingSeatRepository.findByBookingId(booking.getId()).stream()
                .map(bookingSeat -> new BookedSeatResponse(
                        bookingSeat.getShowSeatAllocation().getSeat().getSeatNumber(),
                        bookingSeat.getShowSeatAllocation().getSeat().getZone().name()))
                .toList();
        Show show = booking.getShow();
        return new BookingLookupResponse(booking.getId(), booking.getBookingReference(), booking.getStatus(),
                show.getMovie().getTitle(), show.getShowDate(), show.getStartTime(), booking.getCustomerName(),
                booking.getAdultCount(), booking.getChildCount(), booking.getTicketsUsed(), booking.remainingTickets(), seats);
    }

    private Booking requireBooking(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
    }
}
