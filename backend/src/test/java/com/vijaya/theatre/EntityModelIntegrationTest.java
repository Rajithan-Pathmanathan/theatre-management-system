package com.vijaya.theatre;

import com.vijaya.theatre.auth.StaffAccount;
import com.vijaya.theatre.auth.StaffAccountRepository;
import com.vijaya.theatre.auth.StaffRole;
import com.vijaya.theatre.booking.Booking;
import com.vijaya.theatre.booking.BookingChannel;
import com.vijaya.theatre.booking.BookingRepository;
import com.vijaya.theatre.booking.BookingSeat;
import com.vijaya.theatre.booking.BookingSeatRepository;
import com.vijaya.theatre.booking.BookingStatus;
import com.vijaya.theatre.movie.Movie;
import com.vijaya.theatre.movie.MovieRepository;
import com.vijaya.theatre.movie.MovieStatus;
import com.vijaya.theatre.payment.PaymentGateway;
import com.vijaya.theatre.payment.PaymentStatus;
import com.vijaya.theatre.payment.PaymentTransaction;
import com.vijaya.theatre.payment.PaymentTransactionRepository;
import com.vijaya.theatre.show.AllocationChannel;
import com.vijaya.theatre.show.AllocationStatus;
import com.vijaya.theatre.show.Seat;
import com.vijaya.theatre.show.SeatRepository;
import com.vijaya.theatre.show.SeatZone;
import com.vijaya.theatre.show.Show;
import com.vijaya.theatre.show.ShowRepository;
import com.vijaya.theatre.show.ShowSeatAllocation;
import com.vijaya.theatre.show.ShowSeatAllocationRepository;
import com.vijaya.theatre.show.ShowStatus;
import com.vijaya.theatre.validation.BookingValidation;
import com.vijaya.theatre.validation.BookingValidationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persists one full graph across every Phase 1 entity — movie, show, seat
 * allocation, staff account, booking, booking seat, payment transaction,
 * booking validation — against a real Postgres (Testcontainers). Exercises
 * the Flyway migration and every JPA mapping/foreign key together, which is
 * what actually matters for this slice (ddl-auto=validate only checks
 * columns exist; this checks they're usable).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class EntityModelIntegrationTest {

    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private ShowRepository showRepository;
    @Autowired
    private ShowSeatAllocationRepository showSeatAllocationRepository;
    @Autowired
    private StaffAccountRepository staffAccountRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private BookingSeatRepository bookingSeatRepository;
    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;
    @Autowired
    private BookingValidationRepository bookingValidationRepository;

    @Test
    void persistsAndReloadsAFullBookingGraph() {
        Movie movie = new Movie();
        movie.setTitle("Test Feature");
        movie.setLanguage("Tamil");
        movie.setGenre("Drama");
        movie.setDurationMinutes(140);
        movie.setStatus(MovieStatus.SHOWING);
        movie = movieRepository.save(movie);

        Seat seat = new Seat();
        seat.setSeatNumber("C12");
        seat.setRowLabel("C");
        seat.setSeatInRow(12);
        seat.setZone(SeatZone.MIDDLE);
        seat = seatRepository.save(seat);

        Show show = new Show();
        show.setMovie(movie);
        show.setShowDate(LocalDate.now().plusDays(1));
        show.setStartTime(LocalTime.of(18, 30));
        show.setStatus(ShowStatus.SCHEDULED);
        show.setAdultPrice(new BigDecimal("450.00"));
        show.setChildPrice(new BigDecimal("300.00"));
        show.setGuaranteed(true);
        show.setOnlineBookingEnabled(true);
        show.setOnlineAllocationPercent(40);
        show = showRepository.save(show);

        ShowSeatAllocation allocation = new ShowSeatAllocation();
        allocation.setShow(show);
        allocation.setSeat(seat);
        allocation.setChannel(AllocationChannel.ONLINE);
        allocation.setStatus(AllocationStatus.BOOKED);
        allocation = showSeatAllocationRepository.save(allocation);

        StaffAccount counterStaff = new StaffAccount();
        counterStaff.setUsername("counter1");
        counterStaff.setPasswordHash("{bcrypt}placeholder");
        counterStaff.setFullName("Kamal Perera");
        counterStaff.setRole(StaffRole.COUNTER_STAFF);
        counterStaff = staffAccountRepository.save(counterStaff);

        Booking booking = new Booking();
        booking.setBookingReference("VJY-TEST-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        booking.setShow(show);
        booking.setChannel(BookingChannel.ONLINE);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCustomerName("Nimal Silva");
        booking.setCustomerPhone("+94771234567");
        booking.setAdultCount(1);
        booking.setChildCount(0);
        booking.setTotalAmount(new BigDecimal("450.00"));
        booking.setQrToken(UUID.randomUUID());
        booking = bookingRepository.save(booking);

        BookingSeat bookingSeat = new BookingSeat();
        bookingSeat.setBooking(booking);
        bookingSeat.setShowSeatAllocation(allocation);
        bookingSeatRepository.save(bookingSeat);

        PaymentTransaction payment = new PaymentTransaction();
        payment.setBooking(booking);
        payment.setGateway(PaymentGateway.PAYHERE);
        payment.setAmount(new BigDecimal("450.00"));
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentTransactionRepository.save(payment);

        BookingValidation validation = new BookingValidation();
        validation.setBooking(booking);
        validation.setValidatedBy(counterStaff);
        validation.setTicketsValidated(1);
        bookingValidationRepository.save(validation);
        booking.setTicketsUsed(1);
        bookingRepository.save(booking);

        UUID bookingId = booking.getId();

        assertThat(bookingRepository.findByBookingReference(booking.getBookingReference())).isPresent();
        assertThat(bookingSeatRepository.findByBookingId(bookingId)).hasSize(1);
        assertThat(paymentTransactionRepository.findByBookingId(bookingId)).isPresent();
        assertThat(bookingValidationRepository.findByBookingId(bookingId)).hasSize(1);

        Booking reloaded = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(reloaded.isFullyCheckedIn()).isTrue();
        assertThat(reloaded.remainingTickets()).isZero();
    }
}
