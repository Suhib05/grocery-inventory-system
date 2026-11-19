package com.grocery.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * A product in stock. Status is never stored; it is derived from quantity,
 * minimum stock and expiry date so it can't go stale.
 */
public record Product(
        int id,
        String name,
        int categoryId,
        String categoryName,
        int quantity,
        BigDecimal price,
        int minStock,
        LocalDate expiryDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    /** Products expiring within this many days (inclusive) are "expiring soon". */
    public static final int EXPIRING_SOON_DAYS = 7;

    /**
     * Status priority: out of stock, expired, expiring soon, low stock, in stock.
     * Out of stock wins because there is nothing left on the shelf to expire.
     */
    public ProductStatus statusOn(LocalDate today) {
        if (quantity <= 0)                                   return ProductStatus.OUT_OF_STOCK;
        if (isExpiredOn(today))                              return ProductStatus.EXPIRED;
        if (isExpiringSoonOn(today, EXPIRING_SOON_DAYS))     return ProductStatus.EXPIRING_SOON;
        if (quantity <= minStock)                            return ProductStatus.LOW_STOCK;
        return ProductStatus.IN_STOCK;
    }

    public boolean isExpiredOn(LocalDate today) {
        return expiryDate != null && expiryDate.isBefore(today);
    }

    /** True if the product expires today or within the next {@code days} days. */
    public boolean isExpiringSoonOn(LocalDate today, int days) {
        return expiryDate != null
                && !expiryDate.isBefore(today)
                && !expiryDate.isAfter(today.plusDays(days));
    }

    /** Days until expiry (negative once expired), or null for non-perishables. */
    public Long daysUntilExpiry(LocalDate today) {
        return expiryDate == null ? null : ChronoUnit.DAYS.between(today, expiryDate);
    }

    /** Stock value at the current shelf price. */
    public BigDecimal stockValue() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
