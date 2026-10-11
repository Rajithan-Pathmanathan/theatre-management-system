package com.vijaya.theatre.common.exception;

/** Thrown when a requested entity (movie, show, booking, ...) does not exist. Maps to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
