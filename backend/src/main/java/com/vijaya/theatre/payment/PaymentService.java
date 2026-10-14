package com.vijaya.theatre.payment;

import com.vijaya.theatre.booking.Booking;
import com.vijaya.theatre.booking.BookingRepository;
import com.vijaya.theatre.booking.BookingSeatRepository;
import com.vijaya.theatre.booking.BookingStatus;
import com.vijaya.theatre.common.exception.BusinessRuleViolationException;
import com.vijaya.theatre.common.exception.ResourceNotFoundException;
import com.vijaya.theatre.payment.dto.PayHereNotifyRequest;
import com.vijaya.theatre.payment.dto.PaymentInitiationResponse;
import com.vijaya.theatre.show.AllocationStatus;
import com.vijaya.theatre.show.ShowSeatAllocation;
import com.vijaya.theatre.show.ShowSeatAllocationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * UC-09. Bridges a PENDING_PAYMENT booking to CONFIRMED via PayHere's hosted
 * checkout: {@link #initiatePayment} hands the frontend a signed form payload,
 * {@link #handleNotification} is PayHere's server-to-server callback that
 * actually confirms the booking and finalizes the held seats.
 */
@Service
@Transactional
class PaymentService {

    private static final String CURRENCY = "LKR";
    private static final String GUEST_EMAIL_FALLBACK = "guest@vijayatheatre.lk"; // PayHere requires an email; guest checkout doesn't

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowSeatAllocationRepository showSeatAllocationRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final String merchantId;
    private final String merchantSecret;
    private final String checkoutBaseUrl;
    private final String returnUrl;
    private final String cancelUrl;
    private final String notifyUrl;
    private final List<BookingAddOns> addOns; // BR-13 — empty when no add-on module is present

    PaymentService(BookingRepository bookingRepository, BookingSeatRepository bookingSeatRepository,
                   ShowSeatAllocationRepository showSeatAllocationRepository,
                   PaymentTransactionRepository paymentTransactionRepository,
                   @Value("${app.payhere.merchant-id}") String merchantId,
                   @Value("${app.payhere.merchant-secret}") String merchantSecret,
                   @Value("${app.payhere.checkout-base-url}") String checkoutBaseUrl,
                   @Value("${app.payhere.return-url}") String returnUrl,
                   @Value("${app.payhere.cancel-url}") String cancelUrl,
                   @Value("${app.payhere.notify-url}") String notifyUrl,
                   List<BookingAddOns> addOns) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.showSeatAllocationRepository = showSeatAllocationRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.merchantId = merchantId;
        this.merchantSecret = merchantSecret;
        this.checkoutBaseUrl = checkoutBaseUrl;
        this.returnUrl = returnUrl;
        this.cancelUrl = cancelUrl;
        this.notifyUrl = notifyUrl;
        this.addOns = addOns;
    }

    PaymentInitiationResponse initiatePayment(UUID bookingId) {
        Booking booking = requireBooking(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessRuleViolationException("This booking is not awaiting payment.");
        }
        if (booking.getExpiresAt() == null || booking.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessRuleViolationException("The seat lock for this booking has expired.");
        }

        // BR-13 — tickets plus any extras (snacks) chosen on the payment page; booking.totalAmount stays tickets-only for reporting
        BigDecimal total = booking.getTotalAmount();
        for (BookingAddOns addOn : addOns) {
            total = total.add(addOn.chargeWithBooking(bookingId));
        }

        PaymentTransaction transaction = paymentTransactionRepository.findByBookingId(bookingId)
                .orElseGet(PaymentTransaction::new);
        transaction.setBooking(booking);
        transaction.setGateway(PaymentGateway.PAYHERE);
        transaction.setAmount(total);
        transaction.setCurrency(CURRENCY);
        transaction.setStatus(PaymentStatus.INITIATED);
        paymentTransactionRepository.save(transaction);

        String amount = total.setScale(2, RoundingMode.HALF_UP).toString();
        String hash = PayHereHashUtil.requestHash(merchantId, booking.getBookingReference(), amount, CURRENCY, merchantSecret);
        String[] name = splitCustomerName(booking.getCustomerName());
        String email = booking.getCustomerEmail() != null ? booking.getCustomerEmail() : GUEST_EMAIL_FALLBACK;

        // The booking id rides on return/cancel as a query param (PayHere doesn't echo it back
        // itself) so the frontend's post-redirect page knows which booking to poll.
        String bookingReturnUrl = returnUrl + "?bookingId=" + booking.getId();
        String bookingCancelUrl = cancelUrl + "?bookingId=" + booking.getId();

        return new PaymentInitiationResponse(checkoutBaseUrl, merchantId, booking.getBookingReference(),
                "Vijaya Theatre booking " + booking.getBookingReference(), amount, CURRENCY, hash,
                name[0], name[1], email, booking.getCustomerPhone(),
                "N/A", "Batticaloa", "Sri Lanka", bookingReturnUrl, bookingCancelUrl, notifyUrl);
    }

    void handleNotification(PayHereNotifyRequest notification) {
        String expectedSignature = PayHereHashUtil.notifySignature(notification.merchantId(), notification.orderId(),
                notification.payhereAmount(), notification.payhereCurrency(), notification.statusCode(), merchantSecret);
        if (!expectedSignature.equalsIgnoreCase(notification.md5sig())) {
            throw new BusinessRuleViolationException("Invalid PayHere notification signature.");
        }

        Booking booking = bookingRepository.findByBookingReference(notification.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Unknown booking reference: " + notification.orderId()));
        PaymentTransaction transaction = paymentTransactionRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No payment transaction found for booking: " + notification.orderId()));

        if (transaction.getStatus() != PaymentStatus.INITIATED) {
            return; // already processed — PayHere may resend the same notification, ignore idempotently
        }

        transaction.setGatewayTransactionId(notification.paymentId());
        transaction.setRawResponse(notification.toString());
        transaction.setCompletedAt(Instant.now());

        if ("2".equals(notification.statusCode())) { // PayHere status_code 2 = payment success
            transaction.setStatus(PaymentStatus.SUCCESS);
            confirmBooking(booking);
            addOns.forEach(addOn -> addOn.bookingPaid(booking.getId()));
        } else {
            transaction.setStatus(PaymentStatus.FAILED);
        }
        paymentTransactionRepository.save(transaction);
    }

    byte[] getQrCode(UUID bookingId) {
        Booking booking = requireBooking(bookingId);
        if (booking.getStatus() != BookingStatus.CONFIRMED || booking.getQrToken() == null) {
            throw new BusinessRuleViolationException("This booking is not confirmed yet.");
        }
        return QrCodeGenerator.generatePng(booking.getBookingReference(), 300);
    }

    private void confirmBooking(Booking booking) {
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(Instant.now());
        booking.setExpiresAt(null);
        booking.setQrToken(UUID.randomUUID());
        bookingRepository.save(booking);

        List<UUID> allocationIds = bookingSeatRepository.findByBookingId(booking.getId()).stream()
                .map(bookingSeat -> bookingSeat.getShowSeatAllocation().getId())
                .toList();
        List<ShowSeatAllocation> allocations = showSeatAllocationRepository.lockAllByIdForUpdate(allocationIds);
        for (ShowSeatAllocation allocation : allocations) {
            allocation.setStatus(AllocationStatus.BOOKED);
            allocation.setLockedUntil(null);
            allocation.setLockToken(null);
        }
        showSeatAllocationRepository.saveAll(allocations);
    }

    private String[] splitCustomerName(String fullName) {
        String[] parts = fullName.trim().split("\\s+", 2);
        return parts.length == 2 ? parts : new String[]{parts[0], parts[0]};
    }

    private Booking requireBooking(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
    }
}
