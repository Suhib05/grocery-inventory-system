package com.grocery.service;

import com.grocery.model.Role;
import com.grocery.model.Sale;
import com.grocery.model.User;
import com.grocery.support.InMemoryProductDao;
import com.grocery.support.InMemorySaleDao;
import com.grocery.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.grocery.support.Fixtures.TODAY;
import static com.grocery.support.Fixtures.product;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaleServiceTest {

    private static final User CASHIER = new User(2, "staff", "Sam", Role.STAFF);

    private InMemoryProductDao products;
    private InMemorySaleDao sales;
    private SaleService service;

    @BeforeEach
    void setUp() {
        products = new InMemoryProductDao()
                .add(product(1, "Milk", 10, "5.49", 2, TODAY.plusDays(5)))
                .add(product(2, "Yogurt", 10, "6.29", 2, TODAY.minusDays(1)));
        sales = new InMemorySaleDao(products);
        service = new SaleService(sales, products, new MutableClock(TODAY));
    }

    @Test
    void recordingASaleUsesTheShelfPriceAndLowersStock() {
        Sale sale = service.record(new SaleInput(1, 3, null), CASHIER);
        assertEquals(new BigDecimal("5.49"), sale.unitPrice());
        assertEquals(new BigDecimal("16.47"), sale.totalAmount());
        assertEquals(TODAY, sale.saleDate());
        assertEquals(7, products.findById(1).orElseThrow().quantity());
    }

    @Test
    void cannotSellMoreThanInStock() {
        ConflictException e = assertThrows(ConflictException.class,
                () -> service.record(new SaleInput(1, 11, null), CASHIER));
        assertTrue(e.getMessage().contains("Only 10"));
        assertEquals(10, products.findById(1).orElseThrow().quantity());
    }

    @Test
    void cannotSellExpiredProducts() {
        assertThrows(ConflictException.class, () -> service.record(new SaleInput(2, 1, null), CASHIER));
    }

    @Test
    void stockRunningOutMidSaleBecomesAConflict() {
        sales.simulateRace = true;
        assertThrows(ConflictException.class, () -> service.record(new SaleInput(1, 1, null), CASHIER));
    }

    @Test
    void validatesInput() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.record(new SaleInput(null, 0, TODAY.plusDays(1)), CASHIER));
        assertEquals(3, e.fieldErrors().size());
    }

    @Test
    void unknownProductIs404() {
        assertThrows(NotFoundException.class, () -> service.record(new SaleInput(42, 1, null), CASHIER));
    }

    @Test
    void reportTotalsIgnoreVoidedSales() {
        sales.addSale(1, 2, "5.00", TODAY, false);
        sales.addSale(1, 1, "5.00", TODAY.minusDays(1), true);
        sales.addSale(1, 4, "2.50", TODAY.minusDays(2), false);
        SaleService.SalesReport r = service.report(TODAY.minusDays(7), TODAY);
        assertEquals(3, r.sales().size());
        assertEquals(new BigDecimal("20.00"), r.revenue());
        assertEquals(6, r.unitsSold());
        assertEquals(2, r.transactions());
    }

    @Test
    void reportRejectsBackwardsOrHugeRanges() {
        assertThrows(ValidationException.class, () -> service.report(TODAY, TODAY.minusDays(1)));
        assertThrows(ValidationException.class, () -> service.report(TODAY.minusYears(2), TODAY));
    }

    @Test
    void voidingRestoresStockAndCanOnlyHappenOnce() {
        Sale sale = service.record(new SaleInput(1, 4, null), CASHIER);
        service.voidSale(sale.id(), CASHIER);
        assertEquals(10, products.findById(1).orElseThrow().quantity());
        assertThrows(ConflictException.class, () -> service.voidSale(sale.id(), CASHIER));
    }

    @Test
    void voidingUnknownSaleIs404() {
        assertThrows(NotFoundException.class, () -> service.voidSale(99, CASHIER));
    }
}
