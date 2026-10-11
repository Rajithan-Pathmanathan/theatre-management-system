package com.vijaya.theatre.booking;

import com.vijaya.theatre.booking.dto.BookedSeatResponse;
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

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * UC-08. The highest-risk module in the project (FR-12, NFR-02) — see
 * {@link ShowSeatAllocationRepository#lockAllByIdForUpdate} for the actual
 * concurrency mechanism this builds on.
 */
@Service
@Transactional
class BookingService {

    private static final DateTimeFormatter REFERENCE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String REFERENCE_SUFFIX_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I — avoids phone-read-back ambiguity
    private static final SecureRandom RANDOM = new SecureRandom();

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

    BookingResponse createBooking(BookingRequest request) {
        Show show = requireShow(request.showId());
        validateShowIsBookable(show);
        validateTicketAndSeatCounts(request);

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
        booking.setBookingReference(generateBookingReference());
        booking.setShow(show);
        booking.setChannel(BookingChannel.ONLINE);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setCustomerName(request.customerName());
        booking.setCustomerPhone(request.customerPhone());
        booking.setCustomerEmail(request.customerEmail());
        booking.setAdultCount(request.adultCount());
        booking.setChildCount(request.childCount());
        booking.setTotalAmount(calculateTotal(show, request));
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

    /** BR-04 — seat count must exactly match ticket count; alternative flow — at least one ticket, and no duplicate seat ids. */
    private void validateTicketAndSeatCounts(BookingRequest request) {
        int ticketCount = request.adultCount() + request.childCount();
        if (ticketCount <= 0) {
            throw new BusinessRuleViolationException("At least one adult or child ticket is required.");
        }
        Set<UUID> distinctSeatIds = new HashSet<>(request.seatAllocationIds());
        if (distinctSeatIds.size() != request.seatAllocationIds().size()) {
            throw new BusinessRuleViolationException("The same seat was selected more than once.");
        }
        if (distinctSeatIds.size() != ticketCount) {
            throw new BusinessRuleViolationException(
                    "Selected seat count (" + distinctSeatIds.size() + ") must match the requested ticket count (" + ticketCount + ").");
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
                .filter(allocation -> !isLockable(allocation, now) || allocation.getChannel() != AllocationChannel.ONLINE
                        || !allocation.getShow().getId().equals(show.getId()))
                .map(allocation -> allocation.getSeat().getSeatNumber())
                .toList();

        if (!unavailable.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "The following seats are no longer available: " + String.join(", ", unavailable));
        }
    }

    private boolean isLockable(ShowSeatAllocation allocation, Instant now) {
        return allocation.getStatus() == AllocationStatus.AVAILABLE
                || (allocation.getStatus() == AllocationStatus.LOCKED && allocation.getLockedUntil().isBefore(now));
    }

    private BigDecimal calculateTotal(Show show, BookingRequest request) {
        return show.getAdultPrice().multiply(BigDecimal.valueOf(request.adultCount()))
                .add(show.getChildPrice().multiply(BigDecimal.valueOf(request.childCount())));
    }

    /** Not cryptographically unique by construction, but collision is astronomically unlikely at this scale; the DB's unique constraint is the real backstop. */
    private String generateBookingReference() {
        StringBuilder suffix = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            suffix.append(REFERENCE_SUFFIX_CHARS.charAt(RANDOM.nextInt(REFERENCE_SUFFIX_CHARS.length())));
        }
        return "VJY-" + REFERENCE_DATE.format(LocalDateTime.now()) + "-" + suffix;
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

    private Booking requireBooking(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
    }
}
