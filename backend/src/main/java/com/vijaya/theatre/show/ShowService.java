package com.vijaya.theatre.show;

import com.vijaya.theatre.common.exception.BusinessRuleViolationException;
import com.vijaya.theatre.common.exception.ResourceNotFoundException;
import com.vijaya.theatre.movie.Movie;
import com.vijaya.theatre.movie.MovieRepository;
import com.vijaya.theatre.movie.MovieStatus;
import com.vijaya.theatre.show.dto.PublicSeatResponse;
import com.vijaya.theatre.show.dto.PublicShowResponse;
import com.vijaya.theatre.show.dto.ShowRequest;
import com.vijaya.theatre.show.dto.ShowResponse;
import com.vijaya.theatre.show.dto.ShowStatusRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** UC-05 (admin) and UC-07 (public browsing). */
@Service
@Transactional
class ShowService {

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final ShowMapper showMapper;
    private final SeatAllocationService seatAllocationService;
    private final ShowSeatAllocationRepository showSeatAllocationRepository;
    private final PublicSeatMapper publicSeatMapper;

    ShowService(ShowRepository showRepository, MovieRepository movieRepository, ShowMapper showMapper,
                SeatAllocationService seatAllocationService, ShowSeatAllocationRepository showSeatAllocationRepository,
                PublicSeatMapper publicSeatMapper) {
        this.showRepository = showRepository;
        this.movieRepository = movieRepository;
        this.showMapper = showMapper;
        this.seatAllocationService = seatAllocationService;
        this.showSeatAllocationRepository = showSeatAllocationRepository;
        this.publicSeatMapper = publicSeatMapper;
    }

    @Transactional(readOnly = true)
    List<ShowResponse> listByDate(LocalDate date) {
        return showRepository.findByShowDate(date).stream().map(showMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    ShowResponse get(UUID id) {
        return showMapper.toResponse(requireShow(id));
    }

    /** UC-07 — only ever lists shows a customer could actually book: SCHEDULED, guaranteed, online-enabled, still in the future. */
    @Transactional(readOnly = true)
    List<PublicShowResponse> listPublicByDate(LocalDate date) {
        return showRepository.findByShowDateAndStatus(date, ShowStatus.SCHEDULED).stream()
                .filter(this::isPubliclyBookable)
                .map(this::toPublicResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    PublicShowResponse getPublicShow(UUID id) {
        return toPublicResponse(requirePubliclyBookableShow(id));
    }

    /** FR-18 — only ONLINE-channel seats are ever exposed to a customer. */
    @Transactional(readOnly = true)
    List<PublicSeatResponse> getPublicSeats(UUID id) {
        Show show = requirePubliclyBookableShow(id);
        Comparator<ShowSeatAllocation> seatOrder = Comparator
                .<ShowSeatAllocation, String>comparing(a -> a.getSeat().getRowLabel())
                .thenComparing(a -> a.getSeat().getSeatInRow());
        return showSeatAllocationRepository.findByShowIdAndChannel(show.getId(), AllocationChannel.ONLINE).stream()
                .sorted(seatOrder)
                .map(publicSeatMapper::toResponse)
                .toList();
    }

    ShowResponse create(ShowRequest request) {
        Movie movie = requireBookableMovie(request.movieId());
        validateBusinessRules(request, movie);
        checkForOverlap(request, movie, null);

        Show show = new Show();
        show.setMovie(movie);
        applyRequest(show, request);
        show.setStatus(ShowStatus.SCHEDULED);
        show = showRepository.save(show);
        seatAllocationService.initializeForShow(show);
        return showMapper.toResponse(show);
    }

    ShowResponse update(UUID id, ShowRequest request) {
        Show show = requireShow(id);
        Movie movie = requireBookableMovie(request.movieId());
        validateBusinessRules(request, movie);
        checkForOverlap(request, movie, id);

        show.setMovie(movie);
        applyRequest(show, request);
        return showMapper.toResponse(showRepository.save(show));
    }

    /** FR-08 — cancel/disable/reschedule. See {@link ShowStatusRequest}'s Javadoc for what this deliberately doesn't do yet. */
    ShowResponse updateStatus(UUID id, ShowStatusRequest request) {
        Show show = requireShow(id);
        show.setStatus(request.status());
        return showMapper.toResponse(showRepository.save(show));
    }

    /** BR-01 — online booking requires guaranteed; alternative flow A3 — non-negative prices are enforced by Bean Validation already. */
    private void validateBusinessRules(ShowRequest request, Movie movie) {
        if (request.onlineBookingEnabled() && !request.guaranteed()) {
            throw new BusinessRuleViolationException(
                    "A show cannot be opened for online booking unless it is marked guaranteed (BR-01).");
        }
        if (movie.getStatus() == MovieStatus.INACTIVE) {
            throw new BusinessRuleViolationException(
                    "Cannot schedule a show for an inactive movie: " + movie.getTitle());
        }
    }

    /**
     * UC-05 alternative flow A1. Single-screen theatre — every SCHEDULED show
     * on the date shares the one auditorium, regardless of movie, so this
     * checks against all of them, not just same-movie shows.
     */
    private void checkForOverlap(ShowRequest request, Movie movie, UUID excludingShowId) {
        LocalTime newStart = request.startTime();
        LocalTime newEnd = newStart.plusMinutes(movie.getDurationMinutes());

        boolean overlaps = showRepository.findByShowDateAndStatus(request.showDate(), ShowStatus.SCHEDULED).stream()
                .filter(existing -> !existing.getId().equals(excludingShowId))
                .anyMatch(existing -> {
                    LocalTime existingStart = existing.getStartTime();
                    LocalTime existingEnd = existingStart.plusMinutes(existing.getMovie().getDurationMinutes());
                    return newStart.isBefore(existingEnd) && existingStart.isBefore(newEnd);
                });

        if (overlaps) {
            throw new BusinessRuleViolationException(
                    "This time slot overlaps an existing show on " + request.showDate() + ".");
        }
    }

    private void applyRequest(Show show, ShowRequest request) {
        show.setShowDate(request.showDate());
        show.setStartTime(request.startTime());
        show.setAdultPrice(request.adultPrice());
        show.setChildPrice(request.childPrice());
        show.setGuaranteed(request.guaranteed());
        show.setOnlineBookingEnabled(request.onlineBookingEnabled());
        show.setOnlineAllocationPercent(request.onlineAllocationPercent());
        show.setHasInterval(request.hasInterval());
    }

    private boolean isPubliclyBookable(Show show) {
        return show.isGuaranteed() && show.isOnlineBookingEnabled()
                && LocalDateTime.of(show.getShowDate(), show.getStartTime()).isAfter(LocalDateTime.now());
    }

    private Show requirePubliclyBookableShow(UUID id) {
        Show show = requireShow(id);
        if (show.getStatus() != ShowStatus.SCHEDULED || !isPubliclyBookable(show)) {
            // Deliberately the same exception/message as "doesn't exist" — the public API doesn't
            // distinguish "not found" from "exists but not open for online booking" (UC-07 A2/A4).
            throw new ResourceNotFoundException("Show not found or not open for online booking: " + id);
        }
        return show;
    }

    private PublicShowResponse toPublicResponse(Show show) {
        long remaining = showSeatAllocationRepository.countByShowIdAndChannelAndStatus(
                show.getId(), AllocationChannel.ONLINE, AllocationStatus.AVAILABLE);
        return new PublicShowResponse(show.getId(), show.getMovie().getId(), show.getMovie().getTitle(),
                show.getShowDate(), show.getStartTime(), show.getAdultPrice(), show.getChildPrice(),
                show.isHasInterval(), remaining);
    }

    private Movie requireBookableMovie(UUID movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found: " + movieId));
    }

    private Show requireShow(UUID id) {
        return showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + id));
    }
}
