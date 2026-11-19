package com.grocery.service;

import com.grocery.support.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.grocery.support.Fixtures.TODAY;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginAttemptLimiterTest {

    private final MutableClock clock = new MutableClock(TODAY);
    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(2, Duration.ofMinutes(5), clock);

    @Test
    void allowsUntilTheLimitIsReached() {
        limiter.recordFailure("sam");
        assertDoesNotThrow(() -> limiter.checkAllowed("sam"));
        limiter.recordFailure("sam");
        assertThrows(TooManyAttemptsException.class, () -> limiter.checkAllowed("sam"));
    }

    @Test
    void successResetsTheCounter() {
        limiter.recordFailure("sam");
        limiter.recordSuccess("sam");
        limiter.recordFailure("sam");
        assertDoesNotThrow(() -> limiter.checkAllowed("sam"));
    }

    @Test
    void lockoutExpires() {
        limiter.recordFailure("sam");
        limiter.recordFailure("sam");
        clock.advance(Duration.ofMinutes(5).plusSeconds(1));
        assertDoesNotThrow(() -> limiter.checkAllowed("sam"));
    }

    @Test
    void usersAreTrackedSeparately() {
        limiter.recordFailure("sam");
        limiter.recordFailure("sam");
        assertDoesNotThrow(() -> limiter.checkAllowed("alex"));
    }
}
