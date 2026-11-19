package com.grocery.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Product fields as sent by the client. Boxed types so "missing" can be told apart from 0. */
public record ProductInput(
        String name,
        Integer categoryId,
        Integer quantity,
        BigDecimal price,
        Integer minStock,
        LocalDate expiryDate) { }
