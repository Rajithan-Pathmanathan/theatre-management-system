package com.vijaya.theatre.counter;

import com.vijaya.theatre.auth.StaffAccount;
import com.vijaya.theatre.auth.StaffAccountRepository;
import com.vijaya.theatre.booking.Booking;
import com.vijaya.theatre.booking.BookingChannel;
import com.vijaya.theatre.booking.BookingReferenceGenerator;
import com.vijaya.theatre.booking.BookingRepository;
import com.vijaya.theatre.booking.BookingSeat;
import com.vijaya.theatre.booking.BookingSeatRepository;
import com.vijaya.theatre.booking.BookingStatus;
import com.vijaya.theatre.booking.TicketSeatCountValidator;
import com.vijaya.theatre.booking.dto.BookedSeatResponse;
import com.vijaya.theatre.booking.dto.BookingResponse;
import com.vijaya.theatre.common.exception.BusinessRuleViolationException;
import com.vijaya.theatre.common.exception.ResourceNotFoundException;
import com.vijaya.theatre.counter.dto.CounterSaleRequest;
import com.vijaya.theatre.show.AllocationChannel;
import com.vijaya.theatre.show.AllocationStatus;
import com.vijaya.theatre.show.Show;
import com.vijaya.theatre.show.ShowRepository;
import com.vijaya.theatre.show.ShowSeatAllocation;
import com.vijaya.theatre.show.ShowSeatAllocationRepository;
import com.vijaya.theatre.show.ShowStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * UC-10, FR-66–FR-68, BR-07, BR-17. A physical sale settles on the spot
 * (cash/card in hand), so unlike {@code BookingService} there is no
 * PENDING_PAYMENT/lock window — the booking is created CONFIRMED and the
 * seats go straight to BOOKED.
 */
@Service
@Transactional
class CounterSaleService {

    private static final String DEFAULT_CUSTOMER_NAME = "Walk-in Customer";
    private static final String DEFAULT_CUSTOMER_PHONE = "N/A";

    private final ShowRepository showRepository;
    private final ShowSeatAllocationRepository showSeatAllocationRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final StaffAccountRepository staffAccountRepository;

    CounterSaleService(ShowRepository showRepository, ShowSeatAllocationRepository showSeatAllocationRepository,
                        BookingRepository bookingRepository, BookingSeatRepository bookingSeatRepository,
                        StaffAccountRepository staffAccountRepository) {
        this.showRepository = showRepository;
        this.showSeatAllocationRepository = showSeatAllocationRepository;
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.staffAccountRepository = staffAccountRepository;
    }

    BookingResponse sellTicket(UUID staffId, CounterSaleRequest request) {
        Show show = requireShow(request.showId());
        validateShowIsSellable(show);
        TicketSeatCountValidator.validate(request.adultCount(), request.childCount(), request.seatAllocationIds());

        List<ShowSeatAllocation> allocations = showSeatAllocationRepository.lockAllByIdForUpdate(request.seatAllocationIds());
        validateAllocationsAreSellable(allocations, request, show);

        for (ShowSeatAllocation allocation : allocations) {
            allocation.setStatus(AllocationStatus.BOOKED);
            allocation.setLockedUntil(null);
            allocation.setLockToken(null);
        }
        showSeatAllocationRepository.saveAll(allocations);

        StaffAccount staff = staffAccountRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff account not found: " + staffId));

        Booking booking = new Booking();
        booking.setBookingReference(BookingReferenceGenerator.generate());
        booking.setShow(show);
        booking.setChannel(BookingChannel.COUNTER);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCustomerName(blankToDefault(request.customerName(), DEFAULT_CUSTOMER_NAME));
        booking.setCustomerPhone(blankToDefault(request.customerPhone(), DEFAULT_CUSTOMER_PHONE));
        booking.setAdultCount(request.adultCount());
        booking.setChildCount(request.childCount());
        booking.setTotalAmount(show.calculateTotal(request.adultCount(), request.childCount()));
        booking.setConfirmedAt(Instant.now());
        booking.setQrToken(UUID.randomUUID());
        booking.setPhysicalPaymentMethod(request.physicalPaymentMethod());
        booking.setSoldByStaff(staff);
        Booking savedBooking = bookingRepository.save(booking);

        List<BookingSeat> bookingSeats = allocations.stream().map(allocation -> {
            BookingSeat bookingSeat = new BookingSeat();
            bookingSeat.setBooking(savedBooking);
            bookingSeat.setShowSeatAllocation(allocation);
            return bookingSeat;
        }).toList();
        bookingSeatRepository.saveAll(bookingSeats);

        return toResponse(savedBooking, allocations);
    }

    private void validateShowIsSellable(Show show) {
        if (show.getStatus() != ShowStatus.SCHEDULED) {
            throw new BusinessRuleViolationException("This show is no longer available for sale.");
        }
        if (!LocalDateTime.of(show.getShowDate(), show.getStartTime()).isAfter(LocalDateTime.now())) {
            throw new BusinessRuleViolationException("This show has already started or passed.");
        }
    }

    private void validateAllocationsAreSellable(List<ShowSeatAllocation> allocations, CounterSaleRequest request, Show show) {
        if (allocations.size() != request.seatAllocationIds().size()) {
            throw new BusinessRuleViolationException("One or more selected seats do not exist for this show.");
        }

        Instant now = Instant.now();
        List<String> unavailable = allocations.stream()
                .filter(allocation -> !allocation.isAvailableForSale(now) || allocation.getChannel() != AllocationChannel.PHYSICAL
                        || !allocation.getShow().getId().equals(show.getId()))
                .map(allocation -> allocation.getSeat().getSeatNumber())
                .toList();

        if (!unavailable.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "The following seats are no longer available: " + String.join(", ", unavailable));
        }
    }

    private String blankToDefault(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private BookingResponse toResponse(Booking booking, List<ShowSeatAllocation> allocations) {
        List<BookedSeatResponse> seats = allocations.stream()
                .map(a -> new BookedSeatResponse(a.getSeat().getSeatNumber(), a.getSeat().getZone().name()))
                .toList();
        Show show = booking.getShow();
        return new BookingResponse(booking.getId(), booking.getBookingReference(), booking.getStatus(),
                show.getId(), show.getMovie().getTitle(), show.getShowDate(), show.getStartTime(),
                booking.getAdultCount(), booking.getChildCount(), booking.getTotalAmount(), seats, booking.getExpiresAt());
    }

    private Show requireShow(UUID showId) {
        return showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + showId));
    }
}
