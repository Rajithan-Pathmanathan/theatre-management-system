package com.vijaya.theatre.canteen;

import com.vijaya.theatre.booking.Booking;
import com.vijaya.theatre.booking.BookingRepository;
import com.vijaya.theatre.booking.BookingStatus;
import com.vijaya.theatre.canteen.dto.BookingSnackRequest;
import com.vijaya.theatre.canteen.dto.CanteenItemSales;
import com.vijaya.theatre.canteen.dto.CanteenSalesReportResponse;
import com.vijaya.theatre.canteen.dto.SnackOrderItemRequest;
import com.vijaya.theatre.canteen.dto.SnackOrderItemResponse;
import com.vijaya.theatre.canteen.dto.SnackOrderRequest;
import com.vijaya.theatre.canteen.dto.SnackOrderResponse;
import com.vijaya.theatre.common.exception.BusinessRuleViolationException;
import com.vijaya.theatre.common.exception.ResourceNotFoundException;
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
 * UC-18, UC-19, BR-08, BR-13–BR-16. Standalone and booking-linked orders
 * share one creation path — see {@link SnackOrder}'s Javadoc for why
 * "booking-linked" here is contextual metadata only, not a combined
 * payment. Fulfilment ({@link #listQueue}, {@link #updateStatus},
 * {@link #updateItemAvailability}) is the canteen staff side of UC-19.
 */
@Service
@Transactional
class SnackOrderService {

    private final SnackOrderRepository snackOrderRepository;
    private final SnackOrderItemRepository snackOrderItemRepository;
    private final SnackItemRepository snackItemRepository;
    private final ShowRepository showRepository;
    private final BookingRepository bookingRepository;

    SnackOrderService(SnackOrderRepository snackOrderRepository, SnackOrderItemRepository snackOrderItemRepository,
                       SnackItemRepository snackItemRepository, ShowRepository showRepository,
                       BookingRepository bookingRepository) {
        this.snackOrderRepository = snackOrderRepository;
        this.snackOrderItemRepository = snackOrderItemRepository;
        this.snackItemRepository = snackItemRepository;
        this.showRepository = showRepository;
        this.bookingRepository = bookingRepository;
    }

    SnackOrderResponse createOrder(SnackOrderRequest request) {
        Booking linkedBooking = null;
        Show show;
        if (request.bookingId() != null) {
            linkedBooking = bookingRepository.findById(request.bookingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + request.bookingId()));
            show = linkedBooking.getShow();
        } else {
            if (request.showId() == null) {
                throw new BusinessRuleViolationException("Either a booking or a show must be specified.");
            }
            show = showRepository.findById(request.showId())
                    .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + request.showId()));
        }

        validatePickupTiming(request.pickupTiming(), show);
        Map<UUID, SnackItem> snackItemsById = resolveAndValidateItems(request.items());

        SnackOrder order = new SnackOrder();
        order.setOrderDate(LocalDate.now());
        order.setShow(show);
        order.setLinkedBooking(linkedBooking);
        order.setCustomerName(request.customerName());
        order.setCustomerPhone(request.customerPhone());
        order.setPickupTiming(request.pickupTiming());
        // BR-13 — a booking still awaiting payment carries its snacks into that payment
        boolean awaitingBookingPayment = linkedBooking != null && linkedBooking.getStatus() == BookingStatus.PENDING_PAYMENT;
        order.setStatus(awaitingBookingPayment ? SnackOrderStatus.AWAITING_PAYMENT : SnackOrderStatus.PLACED);
        order.setOrderToken(UUID.randomUUID());
        // ponytail: no pessimistic locking on order_number, unlike seat allocation — a rare concurrent
        // collision here just means a 500 the customer retries, not a double-booked seat.
        order.setOrderNumber(nextOrderNumber(order.getOrderDate()));

        List<SnackOrderItem> items = request.items().stream().map(itemRequest -> {
            SnackItem snackItem = snackItemsById.get(itemRequest.snackItemId());
            SnackOrderItem orderItem = new SnackOrderItem();
            orderItem.setSnackItem(snackItem);
            orderItem.setItemName(snackItem.getName());
            orderItem.setUnitPrice(snackItem.getPrice());
            orderItem.setQuantity(itemRequest.quantity());
            return orderItem;
        }).toList();
        order.setTotalAmount(items.stream().map(SnackOrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add));

        SnackOrder saved = snackOrderRepository.save(order);
        items.forEach(item -> item.setSnackOrder(saved));
        snackOrderItemRepository.saveAll(items);

        return toResponse(saved, items);
    }

    /**
     * BR-13. Sets the snacks to be paid with a booking's tickets, replacing any chosen earlier, so a
     * retried or reloaded payment page never charges twice. Returns null when the list is empty.
     */
    SnackOrderResponse setBookingSnacks(UUID bookingId, BookingSnackRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessRuleViolationException("Snacks can only be added to a booking that is awaiting payment.");
        }
        // ponytail: replacing assumes the customer pays from the page they last saved; two tabs paying different
        // snack choices at once could charge for a replaced order. Lock the selection once payment starts if that ever matters.
        for (SnackOrder existing : snackOrderRepository.findByLinkedBookingIdAndStatus(bookingId, SnackOrderStatus.AWAITING_PAYMENT)) {
            snackOrderItemRepository.deleteAll(snackOrderItemRepository.findBySnackOrderId(existing.getId()));
            snackOrderRepository.delete(existing);
        }
        if (request.items().isEmpty()) {
            return null;
        }
        return createOrder(new SnackOrderRequest(bookingId, null, booking.getCustomerName(), booking.getCustomerPhone(),
                request.pickupTiming(), request.items()));
    }

    @Transactional(readOnly = true)
    SnackOrderResponse getById(UUID id) {
        SnackOrder order = requireOrder(id);
        return toResponse(order, snackOrderItemRepository.findBySnackOrderId(id));
    }

    @Transactional(readOnly = true)
    SnackOrderResponse getByOrderNumber(LocalDate orderDate, int orderNumber) {
        SnackOrder order = snackOrderRepository.findByOrderDateAndOrderNumber(orderDate, orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No snack order #" + orderNumber + " on " + orderDate));
        return toResponse(order, snackOrderItemRepository.findBySnackOrderId(order.getId()));
    }

    @Transactional(readOnly = true)
    byte[] getQrCode(UUID id) {
        SnackOrder order = requireOrder(id);
        return QrCodeGenerator.generatePng(order.getOrderToken().toString(), 300);
    }

    /** UC-19 — a BOTH order appears in either timing's queue, since it's eligible for pickup in both windows. */
    @Transactional(readOnly = true)
    List<SnackOrderResponse> listQueue(LocalDate orderDate, PickupTiming pickupTiming, SnackOrderStatus status) {
        return snackOrderRepository.findByOrderDate(orderDate).stream()
                .filter(order -> pickupTiming == null || order.getPickupTiming() == pickupTiming || order.getPickupTiming() == PickupTiming.BOTH)
                .filter(order -> status == null || order.getStatus() == status)
                .sorted(Comparator.comparing(SnackOrder::getOrderNumber))
                .map(order -> toResponse(order, snackOrderItemRepository.findBySnackOrderId(order.getId())))
                .toList();
    }

    /** UC-19 — staff verify pickup by scanning the order's QR. */
    @Transactional(readOnly = true)
    SnackOrderResponse getByOrderToken(UUID orderToken) {
        SnackOrder order = snackOrderRepository.findByOrderToken(orderToken)
                .orElseThrow(() -> new ResourceNotFoundException("No snack order found for this code."));
        return toResponse(order, snackOrderItemRepository.findBySnackOrderId(order.getId()));
    }

    /** UC-19, BR-16 — CLOSED only ever happens via this explicit staff action; there is no automatic expiry. */
    SnackOrderResponse updateStatus(UUID id, SnackOrderStatus newStatus) {
        SnackOrder order = requireOrder(id);
        validateStatusTransition(order.getStatus(), newStatus);
        order.setStatus(newStatus);
        return toResponse(snackOrderRepository.save(order), snackOrderItemRepository.findBySnackOrderId(id));
    }

    /** UC-19, FR-64 — marking an item unavailable (or reverting that) recalculates the order total. */
    SnackOrderResponse updateItemAvailability(UUID orderId, UUID itemId, boolean unavailable) {
        requireOrder(orderId); // 404s cleanly if the order itself doesn't exist, before touching its items
        List<SnackOrderItem> items = snackOrderItemRepository.findBySnackOrderId(orderId);
        SnackOrderItem item = items.stream().filter(i -> i.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found: " + itemId));
        item.setUnavailable(unavailable);
        snackOrderItemRepository.save(item);

        SnackOrder order = requireOrder(orderId);
        BigDecimal adjustedTotal = items.stream()
                .filter(i -> !i.isUnavailable())
                .map(SnackOrderItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(adjustedTotal);
        return toResponse(snackOrderRepository.save(order), items);
    }

    /**
     * FR-57. Orders are pay-at-collection (see {@link SnackOrder}), so only
     * COLLECTED orders count as sales, and unavailable lines were never charged.
     */
    @Transactional(readOnly = true)
    CanteenSalesReportResponse getDailySalesReport(LocalDate date) {
        List<SnackOrder> collected = snackOrderRepository.findByOrderDate(date).stream()
                .filter(order -> order.getStatus() == SnackOrderStatus.COLLECTED)
                .toList();
        // ponytail: one item query per order; fine at BR-15's 300 orders/day ceiling
        Map<String, List<SnackOrderItem>> byItemName = collected.stream()
                .flatMap(order -> snackOrderItemRepository.findBySnackOrderId(order.getId()).stream())
                .filter(item -> !item.isUnavailable())
                .collect(Collectors.groupingBy(SnackOrderItem::getItemName));
        List<CanteenItemSales> items = byItemName.entrySet().stream()
                .map(entry -> new CanteenItemSales(entry.getKey(),
                        entry.getValue().stream().mapToInt(SnackOrderItem::getQuantity).sum(),
                        entry.getValue().stream().map(SnackOrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add)))
                .sorted(Comparator.comparing(CanteenItemSales::itemName))
                .toList();
        BigDecimal total = items.stream().map(CanteenItemSales::sales).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CanteenSalesReportResponse(date, collected.size(), total, items);
    }

    private void validateStatusTransition(SnackOrderStatus current, SnackOrderStatus next) {
        if (current == SnackOrderStatus.AWAITING_PAYMENT) {
            throw new BusinessRuleViolationException("This order is waiting for its booking to be paid.");
        }
        if (current == SnackOrderStatus.COLLECTED || current == SnackOrderStatus.CLOSED) {
            throw new BusinessRuleViolationException("This order is already " + current + " and cannot be changed further.");
        }
        boolean valid = switch (next) {
            case READY -> current == SnackOrderStatus.PLACED;
            case COLLECTED -> current == SnackOrderStatus.PLACED || current == SnackOrderStatus.READY;
            case CLOSED -> true; // BR-16 — staff can manually close any still-active (PLACED/READY) order
            case PLACED, AWAITING_PAYMENT -> false;
        };
        if (!valid) {
            throw new BusinessRuleViolationException("Cannot move an order from " + current + " to " + next + ".");
        }
    }

    /** BR-14 — DURING_INTERVAL/BOTH require the show to actually have an interval. */
    private void validatePickupTiming(PickupTiming timing, Show show) {
        if (timing != PickupTiming.BEFORE_MOVIE && !show.isHasInterval()) {
            throw new BusinessRuleViolationException(
                    "This show has no interval — only Before Movie pickup is available (BR-14).");
        }
    }

    private Map<UUID, SnackItem> resolveAndValidateItems(List<SnackOrderItemRequest> requestedItems) {
        List<UUID> ids = requestedItems.stream().map(SnackOrderItemRequest::snackItemId).distinct().toList();
        List<SnackItem> found = snackItemRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more snack items do not exist.");
        }
        List<String> unavailable = found.stream().filter(item -> !item.isAvailable()).map(SnackItem::getName).toList();
        if (!unavailable.isEmpty()) {
            throw new BusinessRuleViolationException("The following items are no longer available: " + String.join(", ", unavailable));
        }
        return found.stream().collect(Collectors.toMap(SnackItem::getId, item -> item));
    }

    private int nextOrderNumber(LocalDate orderDate) {
        int current = snackOrderRepository.findTopByOrderDateOrderByOrderNumberDesc(orderDate)
                .map(SnackOrder::getOrderNumber).orElse(0);
        return (current % 300) + 1;
    }

    private SnackOrderResponse toResponse(SnackOrder order, List<SnackOrderItem> items) {
        List<SnackOrderItemResponse> itemResponses = items.stream()
                .map(item -> new SnackOrderItemResponse(item.getId(), item.getItemName(), item.getUnitPrice(), item.getQuantity(), item.isUnavailable()))
                .toList();
        Show show = order.getShow();
        Booking linkedBooking = order.getLinkedBooking();
        return new SnackOrderResponse(order.getId(), order.getOrderDate(), order.getOrderNumber(), show.getId(),
                show.getMovie().getTitle(), show.getShowDate(), show.getStartTime(), order.getCustomerName(),
                order.getCustomerPhone(), order.getPickupTiming(), order.getStatus(), order.getTotalAmount(),
                itemResponses, linkedBooking != null ? linkedBooking.getBookingReference() : null, order.getPrepaidAmount());
    }

    private SnackOrder requireOrder(UUID id) {
        return snackOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Snack order not found: " + id));
    }
}
