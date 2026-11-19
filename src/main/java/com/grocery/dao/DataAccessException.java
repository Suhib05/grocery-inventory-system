package com.grocery.dao;

/** Unchecked wrapper for SQL errors, so callers aren't forced to handle SQLException everywhere. */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) { super(message, cause); }
}
