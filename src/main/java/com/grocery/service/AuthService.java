package com.grocery.service;

import com.grocery.dao.UserDao;
import com.grocery.model.User;
import com.grocery.model.UserCredentials;
import com.grocery.util.PasswordHasher;

import java.util.Optional;

/** Checks usernames and passwords, with lockout after repeated failures. */
public class AuthService {

    private final UserDao userDao;
    private final PasswordHasher hasher;
    private final LoginAttemptLimiter limiter;
    /** Hash checked when the username doesn't exist, so response time doesn't reveal valid usernames. */
    private final String dummyHash;

    public AuthService(UserDao userDao, PasswordHasher hasher, LoginAttemptLimiter limiter) {
        this.userDao = userDao;
        this.hasher = hasher;
        this.limiter = limiter;
        this.dummyHash = hasher.hash("not-a-real-password");
    }

    /**
     * @return the user if the credentials are right and the account is active
     * @throws TooManyAttemptsException if the username is locked out
     */
    public Optional<User> login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new ValidationException("Username and password are required.");
        }
        String name = username.trim();
        limiter.checkAllowed(name);

        Optional<UserCredentials> found = userDao.findByUsername(name);
        boolean passwordOk = hasher.verify(password, found.map(UserCredentials::passwordHash).orElse(dummyHash));

        if (found.isPresent() && passwordOk && found.get().active()) {
            limiter.recordSuccess(name);
            User user = found.get().user();
            userDao.updateLastLogin(user.id());
            return Optional.of(user);
        }
        limiter.recordFailure(name);
        return Optional.empty();
    }
}
