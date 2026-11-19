package com.grocery.support;

import com.grocery.dao.UserDao;
import com.grocery.model.UserCredentials;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryUserDao implements UserDao {

    private final Map<String, UserCredentials> users = new HashMap<>();
    public int lastLoginUpdates = 0;

    public void add(UserCredentials c) { users.put(c.user().username(), c); }

    @Override public Optional<UserCredentials> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    @Override public void updateLastLogin(int userId) { lastLoginUpdates++; }
}
