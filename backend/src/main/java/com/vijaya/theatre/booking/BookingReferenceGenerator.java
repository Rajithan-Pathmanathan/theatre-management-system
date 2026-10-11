package com.vijaya.theatre.booking;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** BR-11 — shared by online (UC-08) and counter (UC-10) sales; both write into the same Booking table. */
public final class BookingReferenceGenerator {

    private static final DateTimeFormatter REFERENCE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String SUFFIX_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I — avoids phone-read-back ambiguity
    private static final SecureRandom RANDOM = new SecureRandom();

    private BookingReferenceGenerator() {
    }

    /** Not cryptographically unique by construction, but collision is astronomically unlikely at this scale; the DB's unique constraint is the real backstop. */
    public static String generate() {
        StringBuilder suffix = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            suffix.append(SUFFIX_CHARS.charAt(RANDOM.nextInt(SUFFIX_CHARS.length())));
        }
        return "VJY-" + REFERENCE_DATE.format(LocalDateTime.now()) + "-" + suffix;
    }
}
