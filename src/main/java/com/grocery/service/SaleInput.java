package com.grocery.service;

import java.time.LocalDate;

/** A sale as sent by the client. The price always comes from the product, never the client. */
public record SaleInput(Integer productId, Integer quantity, LocalDate saleDate) { }
