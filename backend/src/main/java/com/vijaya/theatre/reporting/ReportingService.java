package com.vijaya.theatre.reporting;

import com.vijaya.theatre.booking.Booking;
import com.vijaya.theatre.booking.BookingChannel;
import com.vijaya.theatre.booking.BookingRepository;
import com.vijaya.theatre.booking.BookingStatus;
import com.vijaya.theatre.common.exception.ResourceNotFoundException;
import com.vijaya.theatre.reporting.dto.ChannelBreakdown;
import com.vijaya.theatre.reporting.dto.DailyReportResponse;
import com.vijaya.theatre.reporting.dto.ShowReportResponse;
import com.vijaya.theatre.show.Show;
import com.vijaya.theatre.show.ShowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * UC-14, FR-33–FR-39. Phase 1 covers on-screen daily/show-wise reports only
 * (export is FR-48, Phase 2). A booking counts here only once it's
 * CONFIRMED — PENDING_PAYMENT, EXPIRED, CANCELLED and REFUNDED are all
 * excluded, so the totals always tie back to actual realized sales.
 */
@Service
@Transactional(readOnly = true)
class ReportingService {

    private final BookingRepository bookingRepository;
    private final ShowRepository showRepository;

    ReportingService(BookingRepository bookingRepository, ShowRepository showRepository) {
        this.bookingRepository = bookingRepository;
        this.showRepository = showRepository;
    }

    DailyReportResponse getDailyReport(LocalDate date) {
        List<Booking> bookings = bookingRepository.findByShow_ShowDateAndStatus(date, BookingStatus.CONFIRMED);

        Map<UUID, List<Booking>> byShow = bookings.stream().collect(Collectors.groupingBy(b -> b.getShow().getId()));
        List<ShowReportResponse> showReports = byShow.values().stream()
                .map(showBookings -> buildShowReport(showBookings.get(0).getShow(), showBookings))
                .sorted(Comparator.comparing(ShowReportResponse::startTime))
                .toList();

        ChannelBreakdown online = aggregate(bookings, BookingChannel.ONLINE);
        ChannelBreakdown physical = aggregate(bookings, BookingChannel.COUNTER);
        return new DailyReportResponse(date, online.ticketsSold() + physical.ticketsSold(),
                online.collections().add(physical.collections()), online, physical, showReports);
    }

    ShowReportResponse getShowReport(UUID showId) {
        Show show = requireShow(showId);
        List<Booking> bookings = bookingRepository.findByShowIdAndStatus(showId, BookingStatus.CONFIRMED);
        return buildShowReport(show, bookings);
    }

    /** FR-48 (Phase 2 export path). */
    String exportDailyReportCsv(LocalDate date) {
        return ReportCsvWriter.daily(getDailyReport(date));
    }

    /** FR-48 (Phase 2 export path). */
    String exportShowReportCsv(UUID showId) {
        return ReportCsvWriter.show(getShowReport(showId));
    }

    private ShowReportResponse buildShowReport(Show show, List<Booking> bookings) {
        ChannelBreakdown online = aggregate(bookings, BookingChannel.ONLINE);
        ChannelBreakdown physical = aggregate(bookings, BookingChannel.COUNTER);
        return new ShowReportResponse(show.getId(), show.getMovie().getTitle(), show.getShowDate(), show.getStartTime(),
                online.ticketsSold() + physical.ticketsSold(), online.collections().add(physical.collections()), online, physical);
    }

    private ChannelBreakdown aggregate(List<Booking> bookings, BookingChannel channel) {
        List<Booking> filtered = bookings.stream().filter(b -> b.getChannel() == channel).toList();
        int tickets = filtered.stream().mapToInt(b -> b.getAdultCount() + b.getChildCount()).sum();
        BigDecimal collections = filtered.stream().map(Booking::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ChannelBreakdown(tickets, collections);
    }

    private Show requireShow(UUID showId) {
        return showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + showId));
    }
}
