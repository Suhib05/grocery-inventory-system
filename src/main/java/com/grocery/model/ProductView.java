package com.grocery.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** What the API returns for a product: the stored fields plus derived status. */
public record ProductView(
        int id,
        String name,
        int categoryId,
        String categoryName,
        int quantity,
        BigDecimal price,
        int minStock,
        LocalDate expiryDate,
        ProductStatus status,
        String statusLabel,
        Long daysUntilExpiry) {

    public static ProductView of(Product p, LocalDate today) {
        ProductStatus status = p.statusOn(today);
        return new ProductView(p.id(), p.name(), p.categoryId(), p.categoryName(), p.quantity(),
                p.price(), p.minStock(), p.expiryDate(), status, status.label(), p.daysUntilExpiry(today));
    }
}
