package com.grocery.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Locks a username out for a while after too many failed logins, to slow down
 * password guessing. Kept in memory, which is fine for a single app instance.
 */
public class LoginAttemptLimiter {

    private record Attempts(int failures, Instant lockedUntil) { }

    private final int maxFailures;
    private final Duration lockout;
    private final Clock clock;
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public LoginAttemptLimiter(int maxFailures, Duration lockout, Clock clock) {
        this.maxFailures = maxFailures;
        this.lockout = lockout;
        this.clock = clock;
    }

    /** Throws if the username is currently locked out. */
    public void checkAllowed(String username) {
        Attempts a = attempts.get(key(username));
        if (a != null && a.lockedUntil() != null) {
            Instant now = clock.instant();
            if (now.isBefore(a.lockedUntil())) {
                throw new TooManyAttemptsException(Duration.between(now, a.lockedUntil()));
            }
            attempts.remove(key(username), a);
        }
    }

    public void recordFailure(String username) {
        attempts.compute(key(username), (k, a) -> {
            int failures = (a == null ? 0 : a.failures()) + 1;
            Instant lockedUntil = failures >= maxFailures ? clock.instant().plus(lockout) : null;
            return new Attempts(failures, lockedUntil);
        });
    }

    public void recordSuccess(String username) {
        attempts.remove(key(username));
    }

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
