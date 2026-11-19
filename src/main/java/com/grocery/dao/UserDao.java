package com.grocery.dao;

import com.grocery.model.UserCredentials;
import java.util.Optional;

public interface UserDao {
    Optional<UserCredentials> findByUsername(String username);
    void updateLastLogin(int userId);
}
