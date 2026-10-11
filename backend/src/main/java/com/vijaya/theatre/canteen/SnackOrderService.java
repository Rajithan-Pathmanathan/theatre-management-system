package com.vijaya.theatre.canteen;

import com.vijaya.theatre.booking.Booking;
import com.vijaya.theatre.booking.BookingRepository;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * UC-18, BR-08, BR-13–BR-15. Standalone and booking-linked orders share this
 * one creation path — see {@link SnackOrder}'s Javadoc for why "booking-
 * linked" here is contextual metadata only, not a combined payment.
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
        order.setStatus(SnackOrderStatus.PLACED);
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
                .map(item -> new SnackOrderItemResponse(item.getItemName(), item.getUnitPrice(), item.getQuantity(), item.isUnavailable()))
                .toList();
        Show show = order.getShow();
        Booking linkedBooking = order.getLinkedBooking();
        return new SnackOrderResponse(order.getId(), order.getOrderDate(), order.getOrderNumber(), show.getId(),
                show.getMovie().getTitle(), show.getShowDate(), show.getStartTime(), order.getCustomerName(),
                order.getCustomerPhone(), order.getPickupTiming(), order.getStatus(), order.getTotalAmount(),
                itemResponses, linkedBooking != null ? linkedBooking.getBookingReference() : null);
    }

    private SnackOrder requireOrder(UUID id) {
        return snackOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Snack order not found: " + id));
    }
}
