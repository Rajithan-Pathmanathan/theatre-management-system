package com.vijaya.theatre.show;

import com.vijaya.theatre.common.exception.BusinessRuleViolationException;
import com.vijaya.theatre.common.exception.ResourceNotFoundException;
import com.vijaya.theatre.movie.Movie;
import com.vijaya.theatre.movie.MovieRepository;
import com.vijaya.theatre.movie.MovieStatus;
import com.vijaya.theatre.show.dto.ShowRequest;
import com.vijaya.theatre.show.dto.ShowResponse;
import com.vijaya.theatre.show.dto.ShowStatusRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * UC-05. Seat allocation (ShowSeatAllocation rows) is deliberately not
 * initialized here — that's the next slice's job (UC-06), which needs the
 * Seat master-data work this slice doesn't touch.
 */
@Service
@Transactional
class ShowService {

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final ShowMapper showMapper;

    ShowService(ShowRepository showRepository, MovieRepository movieRepository, ShowMapper showMapper) {
        this.showRepository = showRepository;
        this.movieRepository = movieRepository;
        this.showMapper = showMapper;
    }

    @Transactional(readOnly = true)
    List<ShowResponse> listByDate(LocalDate date) {
        return showRepository.findByShowDate(date).stream().map(showMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    ShowResponse get(UUID id) {
        return showMapper.toResponse(requireShow(id));
    }

    ShowResponse create(ShowRequest request) {
        Movie movie = requireBookableMovie(request.movieId());
        validateBusinessRules(request, movie);
        checkForOverlap(request, movie, null);

        Show show = new Show();
        show.setMovie(movie);
        applyRequest(show, request);
        show.setStatus(ShowStatus.SCHEDULED);
        return showMapper.toResponse(showRepository.save(show));
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

    private Movie requireBookableMovie(UUID movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found: " + movieId));
    }

    private Show requireShow(UUID id) {
        return showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + id));
    }
}
