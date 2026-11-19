package com.grocery.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.grocery.support.Fixtures.TODAY;
import static com.grocery.support.Fixtures.product;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductTest {

    @Test
    @DisplayName("Plenty of stock and a far-off expiry is IN_STOCK")
    void inStock() {
        assertEquals(ProductStatus.IN_STOCK, product(1, "Milk", 50, "5.00", 10, TODAY.plusDays(30)).statusOn(TODAY));
    }

    @Test
    void lowStockAtOrBelowMinimum() {
        assertEquals(ProductStatus.LOW_STOCK, product(1, "Milk", 10, "5.00", 10, null).statusOn(TODAY));
        assertEquals(ProductStatus.IN_STOCK, product(1, "Milk", 11, "5.00", 10, null).statusOn(TODAY));
    }

    @Test
    @DisplayName("Out of stock wins over expired: there is nothing left to expire")
    void outOfStockBeatsExpired() {
        assertEquals(ProductStatus.OUT_OF_STOCK, product(1, "Milk", 0, "5.00", 10, TODAY.minusDays(3)).statusOn(TODAY));
    }

    @Test
    @DisplayName("Expired beats low stock")
    void expiredBeatsLowStock() {
        assertEquals(ProductStatus.EXPIRED, product(1, "Milk", 2, "5.00", 10, TODAY.minusDays(1)).statusOn(TODAY));
    }

    @Test
    @DisplayName("Expiring soon covers today through 7 days from now")
    void expiringSoonWindow() {
        assertEquals(ProductStatus.EXPIRING_SOON, product(1, "Milk", 50, "5.00", 10, TODAY).statusOn(TODAY));
        assertEquals(ProductStatus.EXPIRING_SOON, product(1, "Milk", 50, "5.00", 10, TODAY.plusDays(7)).statusOn(TODAY));
        assertEquals(ProductStatus.IN_STOCK, product(1, "Milk", 50, "5.00", 10, TODAY.plusDays(8)).statusOn(TODAY));
    }

    @Test
    void nonPerishablesNeverExpire() {
        Product p = product(1, "Soap", 50, "3.00", 10, null);
        assertFalse(p.isExpiredOn(TODAY));
        assertFalse(p.isExpiringSoonOn(TODAY, 7));
        assertNull(p.daysUntilExpiry(TODAY));
    }

    @Test
    void daysUntilExpiryIsNegativeOnceExpired() {
        assertEquals(-3L, product(1, "Milk", 5, "5.00", 1, TODAY.minusDays(3)).daysUntilExpiry(TODAY));
        assertEquals(4L, product(1, "Milk", 5, "5.00", 1, TODAY.plusDays(4)).daysUntilExpiry(TODAY));
    }

    @Test
    void stockValueIsPriceTimesQuantity() {
        assertEquals(new BigDecimal("16.47"), product(1, "Milk", 3, "5.49", 1, null).stockValue());
    }

    @Test
    void onlyInStockNeedsNoAttention() {
        assertFalse(ProductStatus.IN_STOCK.needsAttention());
        assertTrue(ProductStatus.EXPIRED.needsAttention());
    }
}
