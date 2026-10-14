package com.vijaya.theatre.canteen;

import com.vijaya.theatre.payment.BookingAddOns;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** BR-13. Snacks ordered on the booking's payment page ride the ticket payment. */
@Component
class CanteenBookingAddOns implements BookingAddOns {

    private final SnackOrderRepository snackOrderRepository;

    CanteenBookingAddOns(SnackOrderRepository snackOrderRepository) {
        this.snackOrderRepository = snackOrderRepository;
    }

    @Override
    public BigDecimal chargeWithBooking(UUID bookingId) {
        List<SnackOrder> orders = awaiting(bookingId);
        orders.forEach(order -> order.setPrepaidAmount(order.getTotalAmount()));
        snackOrderRepository.saveAll(orders);
        return orders.stream().map(SnackOrder::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public void bookingPaid(UUID bookingId) {
        List<SnackOrder> orders = awaiting(bookingId);
        // An order with no prepaid amount was added after payment started, so it wasn't charged: pay at collection.
        orders.forEach(order -> order.setStatus(SnackOrderStatus.PLACED));
        snackOrderRepository.saveAll(orders);
    }

    private List<SnackOrder> awaiting(UUID bookingId) {
        return snackOrderRepository.findByLinkedBookingIdAndStatus(bookingId, SnackOrderStatus.AWAITING_PAYMENT);
    }
}
