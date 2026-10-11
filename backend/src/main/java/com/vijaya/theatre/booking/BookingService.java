package com.vijaya.theatre.booking;

import com.vijaya.theatre.booking.dto.BookedSeatResponse;
import com.vijaya.theatre.booking.dto.BookingAdminResponse;
import com.vijaya.theatre.booking.dto.BookingRequest;
import com.vijaya.theatre.booking.dto.BookingResponse;
import com.vijaya.theatre.common.exception.BusinessRuleViolationException;
import com.vijaya.theatre.common.exception.ResourceNotFoundException;
import com.vijaya.theatre.show.AllocationChannel;
import com.vijaya.theatre.show.AllocationStatus;
import com.vijaya.theatre.show.Show;
import com.vijaya.theatre.show.ShowRepository;
import com.vijaya.theatre.show.ShowSeatAllocation;
import com.vijaya.theatre.show.ShowSeatAllocationRepository;
import com.vijaya.theatre.show.ShowStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * UC-08. The highest-risk module in the project (FR-12, NFR-02) — see
 * {@link ShowSeatAllocationRepository#lockAllByIdForUpdate} for the actual
 * concurrency mechanism this builds on.
 */
@Service
@Transactional
class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowRepository showRepository;
    private final ShowSeatAllocationRepository showSeatAllocationRepository;
    private final long seatLockMinutes;

    BookingService(BookingRepository bookingRepository, BookingSeatRepository bookingSeatRepository,
                   ShowRepository showRepository, ShowSeatAllocationRepository showSeatAllocationRepository,
                   @Value("${app.seat-lock.duration-minutes}") long seatLockMinutes) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.showRepository = showRepository;
        this.showSeatAllocationRepository = showSeatAllocationRepository;
        this.seatLockMinutes = seatLockMinutes;
    }

    /** UC-09 polling — the frontend's payment-result page checks this after returning from PayHere's checkout. */
    BookingResponse getBooking(UUID bookingId) {
        Booking booking = requireBooking(bookingId);
        List<ShowSeatAllocation> allocations = bookingSeatRepository.findByBookingId(bookingId).stream()
                .map(BookingSeat::getShowSeatAllocation)
                .toList();
        return toResponse(booking, allocations);
    }

    /** UC-13 — admin booking list/search; every filter is optional. */
    @Transactional(readOnly = true)
    List<BookingAdminResponse> listForAdmin(UUID showId, LocalDate showDate, BookingStatus status, BookingChannel channel, String reference) {
        return bookingRepository.search(showId, showDate, status, channel, reference).stream()
                .map(this::toAdminResponse)
                .toList();
    }

    /** UC-13 — a single booking's admin detail view. */
    @Transactional(readOnly = true)
    BookingAdminResponse getBookingForAdmin(UUID bookingId) {
        return toAdminResponse(requireBooking(bookingId));
    }

    BookingResponse createBooking(BookingRequest request) {
        Show show = requireShow(request.showId());
        validateShowIsBookable(show);
        TicketSeatCountValidator.validate(request.adultCount(), request.childCount(), request.seatAllocationIds());

        List<ShowSeatAllocation> allocations = showSeatAllocationRepository.lockAllByIdForUpdate(request.seatAllocationIds());
        validateAllocationsAreLockable(allocations, request, show);

        Instant lockExpiry = Instant.now().plus(seatLockMinutes, ChronoUnit.MINUTES);
        UUID lockToken = UUID.randomUUID();
        for (ShowSeatAllocation allocation : allocations) {
            allocation.setStatus(AllocationStatus.LOCKED);
            allocation.setLockedUntil(lockExpiry);
            allocation.setLockToken(lockToken);
        }
        showSeatAllocationRepository.saveAll(allocations);

        Booking booking = new Booking();
        booking.setBookingReference(BookingReferenceGenerator.generate());
        booking.setShow(show);
        booking.setChannel(BookingChannel.ONLINE);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setCustomerName(request.customerName());
        booking.setCustomerPhone(request.customerPhone());
        booking.setCustomerEmail(request.customerEmail());
        booking.setAdultCount(request.adultCount());
        booking.setChildCount(request.childCount());
        booking.setTotalAmount(show.calculateTotal(request.adultCount(), request.childCount()));
        booking.setExpiresAt(lockExpiry);
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

    /** UC-05 alt-flow boundary carried into UC-08 — BR-01, plus the show must still be in the future and not cancelled/disabled. */
    private void validateShowIsBookable(Show show) {
        if (show.getStatus() != ShowStatus.SCHEDULED) {
            throw new BusinessRuleViolationException("This show is no longer available for booking.");
        }
        if (!show.isGuaranteed() || !show.isOnlineBookingEnabled()) {
            throw new BusinessRuleViolationException("This show is not open for online booking (BR-01).");
        }
        if (!LocalDateTime.of(show.getShowDate(), show.getStartTime()).isAfter(LocalDateTime.now())) {
            throw new BusinessRuleViolationException("This show has already started or passed.");
        }
    }

    /**
     * FR-12, NFR-02, BR-03. Called after {@code lockAllByIdForUpdate} has
     * already taken row locks on every allocation that does exist — this
     * only validates what the caller asked for is actually usable. A LOCKED
     * row whose hold has expired is treated as reclaimable here (lazy
     * expiry) rather than waiting for {@link SeatLockExpirySweep}'s next
     * run, so a customer is never blocked by someone else's abandoned
     * attempt just because the sweep hasn't fired yet.
     */
    private void validateAllocationsAreLockable(List<ShowSeatAllocation> allocations, BookingRequest request, Show show) {
        if (allocations.size() != request.seatAllocationIds().size()) {
            throw new BusinessRuleViolationException("One or more selected seats do not exist for this show.");
        }

        Instant now = Instant.now();
        List<String> unavailable = allocations.stream()
                .filter(allocation -> !allocation.isAvailableForSale(now) || allocation.getChannel() != AllocationChannel.ONLINE
                        || !allocation.getShow().getId().equals(show.getId()))
                .map(allocation -> allocation.getSeat().getSeatNumber())
                .toList();

        if (!unavailable.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "The following seats are no longer available: " + String.join(", ", unavailable));
        }
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

    private BookingAdminResponse toAdminResponse(Booking booking) {
        List<BookedSeatResponse> seats = bookingSeatRepository.findByBookingId(booking.getId()).stream()
                .map(bookingSeat -> new BookedSeatResponse(
                        bookingSeat.getShowSeatAllocation().getSeat().getSeatNumber(),
                        bookingSeat.getShowSeatAllocation().getSeat().getZone().name()))
                .toList();
        Show show = booking.getShow();
        return new BookingAdminResponse(booking.getId(), booking.getBookingReference(), booking.getStatus(), booking.getChannel(),
                show.getId(), show.getMovie().getTitle(), show.getShowDate(), show.getStartTime(),
                booking.getCustomerName(), booking.getCustomerPhone(), booking.getCustomerEmail(),
                booking.getAdultCount(), booking.getChildCount(), booking.getTotalAmount(),
                booking.getTicketsUsed(), booking.remainingTickets(), booking.getPhysicalPaymentMethod(),
                booking.getSoldByStaff() != null ? booking.getSoldByStaff().getUsername() : null,
                seats, booking.getConfirmedAt(), booking.getCreatedAt());
    }

    private Show requireShow(UUID showId) {
        return showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + showId));
    }

    private Booking requireBooking(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
    }
}
