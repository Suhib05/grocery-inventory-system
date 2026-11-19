package com.grocery.model;

import java.io.Serializable;

/**
 * Authenticated user as stored in the HTTP session. Deliberately has no
 * password field so it can never be serialized to a client by accident.
 */
public record User(int id, String username, String fullName, Role role) implements Serializable {

    public boolean isAdmin() { return role == Role.ADMIN; }
}
