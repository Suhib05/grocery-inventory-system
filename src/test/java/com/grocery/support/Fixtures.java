package com.grocery.support;

import com.grocery.model.Product;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class Fixtures {

    public static final LocalDate TODAY = LocalDate.of(2026, 3, 15);

    private Fixtures() { }

    public static Product product(int id, String name, int qty, String price, int minStock, LocalDate expiry) {
        return new Product(id, name, 1, "Dairy & Eggs", qty, new BigDecimal(price), minStock, expiry, null, null);
    }
}
