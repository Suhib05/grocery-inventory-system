package com.grocery.service;

import com.grocery.model.Role;
import com.grocery.model.User;
import com.grocery.model.UserCredentials;
import com.grocery.support.InMemoryUserDao;
import com.grocery.support.MutableClock;
import com.grocery.util.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.grocery.support.Fixtures.TODAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    private final PasswordHasher hasher = new PasswordHasher(1_000);
    private InMemoryUserDao users;
    private MutableClock clock;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserDao();
        users.add(new UserCredentials(new User(1, "admin", "Admin", Role.ADMIN), hasher.hash("Admin@123"), true));
        users.add(new UserCredentials(new User(2, "gone", "Former Staff", Role.STAFF), hasher.hash("pw"), false));
        clock = new MutableClock(TODAY);
        auth = new AuthService(users, hasher, new LoginAttemptLimiter(3, Duration.ofMinutes(5), clock));
    }

    @Test
    void correctCredentialsLogIn() {
        User u = auth.login(" admin ", "Admin@123").orElseThrow();
        assertEquals(Role.ADMIN, u.role());
        assertEquals(1, users.lastLoginUpdates);
    }

    @Test
    void wrongPasswordOrUnknownUserFails() {
        assertTrue(auth.login("admin", "wrong").isEmpty());
        assertTrue(auth.login("nobody", "Admin@123").isEmpty());
    }

    @Test
    void deactivatedAccountsCantLogIn() {
        assertTrue(auth.login("gone", "pw").isEmpty());
    }

    @Test
    void blankCredentialsAreAValidationError() {
        assertThrows(ValidationException.class, () -> auth.login("", "x"));
        assertThrows(ValidationException.class, () -> auth.login("admin", ""));
    }

    @Test
    void locksOutAfterRepeatedFailuresEvenWithTheRightPassword() {
        for (int i = 0; i < 3; i++) auth.login("admin", "wrong");
        assertThrows(TooManyAttemptsException.class, () -> auth.login("admin", "Admin@123"));
        assertThrows(TooManyAttemptsException.class, () -> auth.login("ADMIN", "Admin@123")); // case-insensitive

        clock.advance(Duration.ofMinutes(6));
        assertTrue(auth.login("admin", "Admin@123").isPresent());
    }
}
