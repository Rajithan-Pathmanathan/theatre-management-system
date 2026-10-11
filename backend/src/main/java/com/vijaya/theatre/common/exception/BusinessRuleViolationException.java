package com.vijaya.theatre.common.exception;

/**
 * Thrown when a request is well-formed but violates a business rule from the
 * SRS (e.g. BR-01 booking a non-guaranteed show, BR-04 seat/ticket count
 * mismatch, a seat already locked/booked). Maps to HTTP 409 Conflict.
 */
public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
