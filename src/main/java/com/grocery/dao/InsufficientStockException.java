package com.grocery.dao;

/** Thrown when a conditional stock update finds less stock than requested. */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) { super(message); }
}
