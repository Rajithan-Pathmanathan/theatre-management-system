package com.vijaya.theatre.movie;

import com.vijaya.theatre.common.exception.BusinessRuleViolationException;
import com.vijaya.theatre.common.exception.ResourceNotFoundException;
import com.vijaya.theatre.movie.dto.MovieRequest;
import com.vijaya.theatre.movie.dto.MovieResponse;
import com.vijaya.theatre.movie.dto.MovieStatusRequest;
import com.vijaya.theatre.show.ShowRepository;
import com.vijaya.theatre.show.ShowStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * UC-04. Deliberately skips the "duplicate active title" confirm-before-create
 * flow (alternative flow A3) — that's a client-side confirmation dialog, not
 * server-side business logic; nothing stops two movies sharing a title here.
 */
@Service
@Transactional
class MovieService {

    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;
    private final MovieMapper movieMapper;

    MovieService(MovieRepository movieRepository, ShowRepository showRepository, MovieMapper movieMapper) {
        this.movieRepository = movieRepository;
        this.showRepository = showRepository;
        this.movieMapper = movieMapper;
    }

    @Transactional(readOnly = true)
    List<MovieResponse> list() {
        return movieRepository.findAll().stream().map(movieMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    MovieResponse get(UUID id) {
        return movieMapper.toResponse(requireMovie(id));
    }

    MovieResponse create(MovieRequest request) {
        Movie movie = new Movie();
        movieMapper.updateEntityFromRequest(request, movie);
        movie.setStatus(MovieStatus.UPCOMING);
        return movieMapper.toResponse(movieRepository.save(movie));
    }

    MovieResponse update(UUID id, MovieRequest request) {
        Movie movie = requireMovie(id);
        movieMapper.updateEntityFromRequest(request, movie);
        return movieMapper.toResponse(movieRepository.save(movie));
    }

    /** FR-03. Alternative flow A4 — INACTIVE is refused while a future SCHEDULED show still references this movie. */
    MovieResponse updateStatus(UUID id, MovieStatusRequest request) {
        Movie movie = requireMovie(id);
        if (request.status() == MovieStatus.INACTIVE
                && showRepository.existsByMovieIdAndStatusAndShowDateGreaterThanEqual(id, ShowStatus.SCHEDULED, LocalDate.now())) {
            throw new BusinessRuleViolationException(
                    "Cannot deactivate '" + movie.getTitle() + "' while it still has upcoming scheduled shows. "
                            + "Cancel or reschedule those shows first.");
        }
        movie.setStatus(request.status());
        return movieMapper.toResponse(movieRepository.save(movie));
    }

    private Movie requireMovie(UUID id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found: " + id));
    }
}
