package com.vijaya.theatre.booking;

import com.vijaya.theatre.TestcontainersConfiguration;
import com.vijaya.theatre.auth.StaffAccount;
import com.vijaya.theatre.auth.StaffAccountRepository;
import com.vijaya.theatre.auth.StaffRole;
import com.vijaya.theatre.auth.dto.LoginRequest;
import com.vijaya.theatre.movie.dto.MovieRequest;
import com.vijaya.theatre.show.AllocationChannel;
import com.vijaya.theatre.show.ShowSeatAllocationRepository;
import com.vijaya.theatre.show.dto.ShowRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FR-12, NFR-02 — the single highest-risk guarantee in this project: two
 * customers racing for the same seat must never both win. Deliberately not
 * {@code @Transactional} at the class level (unlike every other integration
 * test here) — wrapping the test in one shared transaction would serialize
 * both "concurrent" requests onto the same connection and prove nothing.
 * Each request below runs in its own real thread, its own real transaction,
 * against the same Testcontainers Postgres instance.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BookingConcurrencyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private StaffAccountRepository staffAccountRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ShowSeatAllocationRepository showSeatAllocationRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private BookingSeatRepository bookingSeatRepository;

    private UUID createdShowId;

    @AfterEach
    void cleanUp() {
        // No @Transactional rollback here (that's the whole point), so clean up explicitly.
        // BookingSeat rows reference Booking with no cascade configured — delete children first.
        if (createdShowId != null) {
            List<Booking> bookings = bookingRepository.findAll().stream()
                    .filter(b -> b.getShow().getId().equals(createdShowId))
                    .toList();
            for (Booking booking : bookings) {
                bookingSeatRepository.findByBookingId(booking.getId()).forEach(bookingSeatRepository::delete);
            }
            bookingRepository.deleteAll(bookings);
        }
    }

    @Test
    void exactlyOneOfTwoSimultaneousBookingAttempts_forTheSameSeat_succeeds() throws Exception {
        String adminToken = loginAsNewAdmin();
        createdShowId = createShow(adminToken);
        UUID contestedSeatId = showSeatAllocationRepository.findByShowId(createdShowId).stream()
                .filter(a -> a.getChannel() == AllocationChannel.ONLINE)
                .findFirst().orElseThrow().getId();

        String bodyA = bookingRequestBody(createdShowId, contestedSeatId, "Customer A", "+94770000001");
        String bodyB = bookingRequestBody(createdShowId, contestedSeatId, "Customer B", "+94770000002");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger createdCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        for (String body : List.of(bodyA, bodyB)) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await(10, TimeUnit.SECONDS);
                    int status = mockMvc.perform(post("/api/bookings")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                            .andReturn().getResponse().getStatus();
                    if (status == 201) {
                        createdCount.incrementAndGet();
                    } else if (status == 409) {
                        conflictCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(10, TimeUnit.SECONDS); // both threads reach the starting line before either fires
        startLatch.countDown();
        boolean finished = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).as("both booking attempts should have completed").isTrue();
        assertThat(createdCount.get()).as("exactly one attempt should win the seat").isEqualTo(1);
        assertThat(conflictCount.get()).as("the other attempt should be rejected, not silently dropped").isEqualTo(1);

        long bookingsForThisShow = bookingRepository.findAll().stream()
                .filter(b -> b.getShow().getId().equals(createdShowId)).count();
        assertThat(bookingsForThisShow).as("only one Booking row should exist for the contested seat").isEqualTo(1);
    }

    private String bookingRequestBody(UUID showId, UUID seatId, String customerName, String phone) {
        return """
                {"showId":"%s","adultCount":1,"childCount":0,"seatAllocationIds":["%s"],
                 "customerName":"%s","customerPhone":"%s","customerEmail":null}
                """.formatted(showId, seatId, customerName, phone);
    }

    private String loginAsNewAdmin() throws Exception {
        String username = "admin-concurrency-" + UUID.randomUUID().toString().substring(0, 8);
        StaffAccount account = new StaffAccount();
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode("a-strong-password"));
        account.setFullName("Test Admin");
        account.setRole(StaffRole.ADMIN);
        account.setActive(true);
        staffAccountRepository.save(account);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, "a-strong-password"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asString();
    }

    private UUID createShow(String adminToken) throws Exception {
        MovieRequest movieRequest = new MovieRequest(
                "Concurrency Test Movie " + UUID.randomUUID(), "Tamil", "Drama", 140, null, null);
        String movieResponse = mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movieRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID movieId = UUID.fromString(objectMapper.readTree(movieResponse).get("id").asString());

        ShowRequest showRequest = new ShowRequest(movieId, LocalDate.now().plusDays(30), LocalTime.of(19, 0),
                new BigDecimal("450.00"), new BigDecimal("300.00"), true, true, 100, true);
        String showResponse = mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(showResponse).get("id").asString());
    }
}
