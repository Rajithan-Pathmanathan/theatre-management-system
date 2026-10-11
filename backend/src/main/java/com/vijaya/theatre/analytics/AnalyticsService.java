package com.vijaya.theatre.analytics;

import com.vijaya.theatre.analytics.dto.MovieAnalyticsResponse;
import com.vijaya.theatre.analytics.dto.ShowOccupancyResponse;
import com.vijaya.theatre.booking.Booking;
import com.vijaya.theatre.booking.BookingChannel;
import com.vijaya.theatre.booking.BookingRepository;
import com.vijaya.theatre.booking.BookingStatus;
import com.vijaya.theatre.common.exception.BusinessRuleViolationException;
import com.vijaya.theatre.common.exception.ResourceNotFoundException;
import com.vijaya.theatre.movie.Movie;
import com.vijaya.theatre.movie.MovieRepository;
import com.vijaya.theatre.reporting.dto.ChannelBreakdown;
import com.vijaya.theatre.show.Show;
import com.vijaya.theatre.show.ShowRepository;
import com.vijaya.theatre.show.ShowSeatAllocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * UC-15, FR-40–FR-45. Historical decision-support only — occupancy,
 * attendance, and channel-performance trends computed directly from
 * Booking at query time. No pre-aggregated summary table: at this
 * project's scale a live query is plenty fast, and the plan explicitly
 * treats that as optional, not required (see DEVELOPMENT_PLAN.md Phase 2).
 */
@Service
@Transactional(readOnly = true)
class AnalyticsService {

    private final BookingRepository bookingRepository;
    private final ShowRepository showRepository;
    private final ShowSeatAllocationRepository showSeatAllocationRepository;
    private final MovieRepository movieRepository;

    AnalyticsService(BookingRepository bookingRepository, ShowRepository showRepository,
                      ShowSeatAllocationRepository showSeatAllocationRepository, MovieRepository movieRepository) {
        this.bookingRepository = bookingRepository;
        this.showRepository = showRepository;
        this.showSeatAllocationRepository = showSeatAllocationRepository;
        this.movieRepository = movieRepository;
    }

    MovieAnalyticsResponse getMovieAnalytics(UUID movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found: " + movieId));
        List<Show> shows = showRepository.findByMovieId(movieId);
        List<Booking> bookings = shows.stream()
                .flatMap(show -> bookingRepository.findByShowIdAndStatus(show.getId(), BookingStatus.CONFIRMED).stream())
                .toList();

        int ticketsSold = ticketsSold(bookings);
        int ticketsUsed = ticketsUsed(bookings);
        BigDecimal collections = bookings.stream().map(Booking::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalSeatCapacity = shows.stream().mapToInt(show -> showSeatAllocationRepository.findByShowId(show.getId()).size()).sum();

        return new MovieAnalyticsResponse(movie.getId(), movie.getTitle(), shows.size(), ticketsSold, collections,
                percentage(ticketsSold, totalSeatCapacity), ticketsUsed, percentage(ticketsUsed, ticketsSold),
                aggregate(bookings, BookingChannel.ONLINE), aggregate(bookings, BookingChannel.COUNTER));
    }

    List<ShowOccupancyResponse> getOccupancyTrend(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessRuleViolationException("'from' date must not be after 'to' date.");
        }
        return showRepository.findByShowDateBetween(from, to).stream()
                .sorted(Comparator.comparing(Show::getShowDate).thenComparing(Show::getStartTime))
                .map(this::buildShowOccupancy)
                .toList();
    }

    private ShowOccupancyResponse buildShowOccupancy(Show show) {
        List<Booking> bookings = bookingRepository.findByShowIdAndStatus(show.getId(), BookingStatus.CONFIRMED);
        int ticketsSold = ticketsSold(bookings);
        int ticketsUsed = ticketsUsed(bookings);
        int totalSeats = showSeatAllocationRepository.findByShowId(show.getId()).size();

        return new ShowOccupancyResponse(show.getId(), show.getMovie().getTitle(), show.getShowDate(), show.getStartTime(),
                totalSeats, ticketsSold, percentage(ticketsSold, totalSeats), ticketsUsed, percentage(ticketsUsed, ticketsSold));
    }

    private int ticketsSold(List<Booking> bookings) {
        return bookings.stream().mapToInt(b -> b.getAdultCount() + b.getChildCount()).sum();
    }

    private int ticketsUsed(List<Booking> bookings) {
        return bookings.stream().mapToInt(Booking::getTicketsUsed).sum();
    }

    private ChannelBreakdown aggregate(List<Booking> bookings, BookingChannel channel) {
        List<Booking> filtered = bookings.stream().filter(b -> b.getChannel() == channel).toList();
        BigDecimal collections = filtered.stream().map(Booking::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ChannelBreakdown(ticketsSold(filtered), collections);
    }

    private double percentage(int numerator, int denominator) {
        if (denominator == 0) {
            return 0;
        }
        return Math.round(numerator * 1000.0 / denominator) / 10.0; // one decimal place
    }
}
