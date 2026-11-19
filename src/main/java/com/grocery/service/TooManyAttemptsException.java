package com.grocery.service;

import java.time.Duration;

public class TooManyAttemptsException extends RuntimeException {

    private final Duration retryAfter;

    public TooManyAttemptsException(Duration retryAfter) {
        super("Too many failed login attempts. Try again in "
                + Math.max(1, retryAfter.toMinutes()) + " minute(s).");
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() { return retryAfter; }
}
