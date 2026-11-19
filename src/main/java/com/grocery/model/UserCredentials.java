package com.grocery.model;

/** A user plus their stored password hash. Only used inside the auth flow. */
public record UserCredentials(User user, String passwordHash, boolean active) { }
