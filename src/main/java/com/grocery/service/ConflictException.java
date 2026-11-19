package com.grocery.service;

/** The request is valid but clashes with current state (e.g. not enough stock). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}
