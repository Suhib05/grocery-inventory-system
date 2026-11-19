package com.grocery.service;

import com.grocery.dao.ProductQuery;
import com.grocery.model.ProductStatus;
import com.grocery.model.ProductView;
import com.grocery.support.InMemoryCategoryDao;
import com.grocery.support.InMemoryProductDao;
import com.grocery.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.grocery.support.Fixtures.TODAY;
import static com.grocery.support.Fixtures.product;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductServiceTest {

    private InMemoryProductDao dao;
    private ProductService service;

    @BeforeEach
    void setUp() {
        dao = new InMemoryProductDao()
                .add(product(1, "Whole Milk", 40, "5.49", 10, TODAY.plusDays(3)))   // expiring soon
                .add(product(2, "Yogurt", 20, "6.29", 10, TODAY.minusDays(2)))      // expired
                .add(product(3, "Butter", 0, "6.49", 8, TODAY.plusDays(60)))        // out of stock
                .add(product(4, "Cheddar", 35, "7.99", 10, TODAY.plusDays(45)));    // in stock
        service = new ProductService(dao, new InMemoryCategoryDao(), new MutableClock(TODAY));
    }

    private static ProductInput input(String name, Integer cat, Integer qty, String price, Integer min) {
        return new ProductInput(name, cat, qty, price == null ? null : new BigDecimal(price), min, null);
    }

    @Test
    void createsAValidProductWithCleanedUpName() {
        ProductView v = service.create(input("  Sourdough   Loaf ", 2, 12, "4.5", 5));
        assertEquals("Sourdough Loaf", v.name());
        assertEquals("Bakery", v.categoryName());
        assertEquals(new BigDecimal("4.50"), v.price());
        assertEquals(ProductStatus.IN_STOCK, v.status());
    }

    @Test
    void minStockDefaultsWhenMissing() {
        assertEquals(ProductService.DEFAULT_MIN_STOCK, service.create(input("Rye", 2, 50, "3.00", null)).minStock());
    }

    @Test
    void reportsEveryInvalidFieldAtOnce() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create(input(" ", 99, -1, "-2", -5)));
        assertEquals(List.of("name", "categoryId", "quantity", "price", "minStock"),
                List.copyOf(e.fieldErrors().keySet()));
    }

    @Test
    void requiresQuantityAndPrice() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create(input("Rye", 2, null, null, 5)));
        assertTrue(e.fieldErrors().containsKey("quantity"));
        assertTrue(e.fieldErrors().containsKey("price"));
    }

    @Test
    void rejectsMoreThanTwoDecimalPlaces() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create(input("Rye", 2, 5, "1.999", 5)));
        assertEquals("Price can have at most 2 decimal places.", e.fieldErrors().get("price"));
    }

    @Test
    void rejectsOverlongNames() {
        String longName = "x".repeat(ProductService.MAX_NAME_LENGTH + 1);
        assertThrows(ValidationException.class, () -> service.create(input(longName, 1, 1, "1.00", 1)));
    }

    @Test
    void updateOfMissingProductIs404() {
        assertThrows(NotFoundException.class, () -> service.update(999, input("X", 1, 1, "1.00", 1)));
    }

    @Test
    void archiveOfMissingProductIs404() {
        assertThrows(NotFoundException.class, () -> service.archive(999));
    }

    @Test
    void filtersByDerivedStatus() {
        List<ProductView> expired = service.list(ProductQuery.all(), ProductStatus.EXPIRED);
        assertEquals(List.of("Yogurt"), expired.stream().map(ProductView::name).toList());
    }

    @Test
    void expiryAlertsSkipOutOfStockAndSortByDate() {
        ProductService.ExpiryAlerts alerts = service.expiryAlerts(7);
        assertEquals(List.of("Yogurt"), alerts.expired().stream().map(ProductView::name).toList());
        assertEquals(List.of("Whole Milk"), alerts.expiringSoon().stream().map(ProductView::name).toList());
    }

    @Test
    void widerAlertWindowFindsMore() {
        assertEquals(2, service.expiryAlerts(50).expiringSoon().size()); // milk + cheddar; butter is out of stock
    }

    @Test
    void alertWindowMustBeSensible() {
        assertThrows(ValidationException.class, () -> service.expiryAlerts(0));
        assertThrows(ValidationException.class, () -> service.expiryAlerts(365));
    }
}
