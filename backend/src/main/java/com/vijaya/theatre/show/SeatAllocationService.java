package com.vijaya.theatre.show;

import com.vijaya.theatre.common.exception.ResourceNotFoundException;
import com.vijaya.theatre.show.dto.SeatAllocationItemResponse;
import com.vijaya.theatre.show.dto.SeatAllocationSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FR-09–FR-12, BR-05, BR-06. Owns three things: creating the 300
 * {@link ShowSeatAllocation} rows for a newly scheduled show
 * ({@link #initializeForShow}, called from {@code ShowService.create}),
 * viewing the current split ({@link #getSummary}), and UC-06's admin
 * reconfiguration of it ({@link #reconfigureAndSummarize}).
 */
@Service
@Transactional
class SeatAllocationService {

    private static final Comparator<Seat> SEAT_ORDER =
            Comparator.comparing(Seat::getRowLabel).thenComparing(Seat::getSeatInRow);

    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;
    private final ShowSeatAllocationRepository showSeatAllocationRepository;
    private final SeatAllocationMapper seatAllocationMapper;

    SeatAllocationService(SeatRepository seatRepository, ShowRepository showRepository,
                           ShowSeatAllocationRepository showSeatAllocationRepository,
                           SeatAllocationMapper seatAllocationMapper) {
        this.seatRepository = seatRepository;
        this.showRepository = showRepository;
        this.showSeatAllocationRepository = showSeatAllocationRepository;
        this.seatAllocationMapper = seatAllocationMapper;
    }

    /** Called once, right after a show is persisted — creates one allocation row per hall seat. */
    void initializeForShow(Show show) {
        List<Seat> allSeats = seatRepository.findAll();
        Set<UUID> onlineSeatIds = computeOnlineSeatIds(groupByZone(allSeats), show.getOnlineAllocationPercent());

        List<ShowSeatAllocation> allocations = allSeats.stream().map(seat -> {
            ShowSeatAllocation allocation = new ShowSeatAllocation();
            allocation.setShow(show);
            allocation.setSeat(seat);
            allocation.setChannel(onlineSeatIds.contains(seat.getId()) ? AllocationChannel.ONLINE : AllocationChannel.PHYSICAL);
            allocation.setStatus(AllocationStatus.AVAILABLE);
            return allocation;
        }).toList();

        showSeatAllocationRepository.saveAll(allocations);
    }

    @Transactional(readOnly = true)
    SeatAllocationSummaryResponse getSummary(UUID showId) {
        Show show = requireShow(showId);
        return buildSummary(show, showSeatAllocationRepository.findByShowId(showId));
    }

    /**
     * UC-06, FR-10, FR-11, BR-06. Recomputes the target online/physical
     * split and applies it — but only to allocations still AVAILABLE; a
     * BOOKED or LOCKED seat keeps its current channel regardless of the new
     * target (FR-11, AC-04).
     * <p>ponytail: this does not account for already-committed ONLINE seats
     * counting toward the new target before redistributing the remainder —
     * it simply recomputes the whole pattern and leaves non-AVAILABLE seats
     * untouched, which can only drift the actual online share slightly away
     * from the requested percentage once real bookings exist. Not visible
     * yet (nothing can be booked until the booking slice lands); revisit if
     * that drift matters once it can happen.
     */
    SeatAllocationSummaryResponse reconfigureAndSummarize(UUID showId, int newOnlineAllocationPercent) {
        Show show = requireShow(showId);
        show.setOnlineAllocationPercent(newOnlineAllocationPercent);

        List<ShowSeatAllocation> allocations = showSeatAllocationRepository.findByShowId(showId);
        Map<SeatZone, List<Seat>> seatsByZone = groupByZone(allocations.stream().map(ShowSeatAllocation::getSeat).toList());
        Set<UUID> targetOnlineSeatIds = computeOnlineSeatIds(seatsByZone, newOnlineAllocationPercent);

        for (ShowSeatAllocation allocation : allocations) {
            if (allocation.getStatus() != AllocationStatus.AVAILABLE) {
                continue; // BR-06 — booked/locked seats are frozen
            }
            allocation.setChannel(targetOnlineSeatIds.contains(allocation.getSeat().getId())
                    ? AllocationChannel.ONLINE : AllocationChannel.PHYSICAL);
        }

        showRepository.save(show);
        return buildSummary(show, showSeatAllocationRepository.saveAll(allocations));
    }

    private SeatAllocationSummaryResponse buildSummary(Show show, List<ShowSeatAllocation> allocations) {
        List<SeatAllocationItemResponse> items = allocations.stream()
                .sorted(Comparator.comparing(ShowSeatAllocation::getSeat, SEAT_ORDER))
                .map(seatAllocationMapper::toItemResponse)
                .toList();
        long onlineCount = items.stream().filter(item -> item.channel() == AllocationChannel.ONLINE).count();
        return new SeatAllocationSummaryResponse(
                show.getId(), show.getOnlineAllocationPercent(), items.size(), onlineCount, items.size() - onlineCount, items);
    }

    private Map<SeatZone, List<Seat>> groupByZone(List<Seat> seats) {
        Map<SeatZone, List<Seat>> byZone = seats.stream().collect(Collectors.groupingBy(Seat::getZone));
        byZone.values().forEach(list -> list.sort(SEAT_ORDER));
        return byZone;
    }

    /**
     * BR-06 — spreads the target seat count evenly across each zone's seat
     * list rather than taking a contiguous block, so online seats land as
     * mixed clusters through the zone instead of one clump at either end.
     */
    private Set<UUID> computeOnlineSeatIds(Map<SeatZone, List<Seat>> seatsByZone, int percent) {
        Set<UUID> onlineSeatIds = new HashSet<>();
        for (List<Seat> zoneSeats : seatsByZone.values()) {
            int target = Math.round(zoneSeats.size() * percent / 100f);
            if (target <= 0 || zoneSeats.isEmpty()) {
                continue;
            }
            double step = (double) zoneSeats.size() / target;
            for (int i = 0; i < target; i++) {
                int index = Math.min((int) Math.round(i * step), zoneSeats.size() - 1);
                onlineSeatIds.add(zoneSeats.get(index).getId());
            }
        }
        return onlineSeatIds;
    }

    private Show requireShow(UUID showId) {
        return showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + showId));
    }
}
