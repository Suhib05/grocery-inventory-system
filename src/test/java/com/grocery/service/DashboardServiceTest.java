package com.grocery.service;

import com.grocery.model.DailyRevenue;
import com.grocery.model.ProductStatus;
import com.grocery.model.ProductView;
import com.grocery.support.InMemoryCategoryDao;
import com.grocery.support.InMemoryProductDao;
import com.grocery.support.InMemorySaleDao;
import com.grocery.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.grocery.support.Fixtures.TODAY;
import static com.grocery.support.Fixtures.product;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardServiceTest {

    private InMemorySaleDao sales;
    private DashboardService service;

    @BeforeEach
    void setUp() {
        InMemoryProductDao products = new InMemoryProductDao()
                .add(product(1, "Milk", 40, "2.00", 10, TODAY.plusDays(2)))   // expiring soon
                .add(product(2, "Eggs", 5, "4.00", 10, TODAY.plusDays(20)))   // low
                .add(product(3, "Butter", 0, "6.00", 5, null))                // out
                .add(product(4, "Yogurt", 3, "1.00", 1, TODAY.minusDays(1))); // expired
        sales = new InMemorySaleDao(products);
        service = new DashboardService(products, new InMemoryCategoryDao(), sales, new MutableClock(TODAY));
    }

    @Test
    void countsProductsByStatus() {
        DashboardService.Summary s = service.summary();
        assertEquals(4, s.totalProducts());
        assertEquals(1, s.statusCounts().get(ProductStatus.EXPIRING_SOON));
        assertEquals(1, s.statusCounts().get(ProductStatus.LOW_STOCK));
        assertEquals(1, s.statusCounts().get(ProductStatus.OUT_OF_STOCK));
        assertEquals(1, s.statusCounts().get(ProductStatus.EXPIRED));
        assertEquals(0, s.statusCounts().get(ProductStatus.IN_STOCK));
    }

    @Test
    void stockValueAddsUpAllProducts() {
        assertEquals(new BigDecimal("103.00"), service.summary().stockValue()); // 80 + 20 + 0 + 3
    }

    @Test
    void lowStockListIncludesOutOfStockFirst() {
        assertEquals(List.of("Butter", "Eggs"),
                service.summary().lowStock().stream().map(ProductView::name).toList());
    }

    @Test
    void revenueChartHasSevenDaysWithZerosFilledIn() {
        sales.addSale(1, 2, "2.00", TODAY, false);
        sales.addSale(1, 1, "2.00", TODAY.minusDays(3), false);
        sales.addSale(1, 9, "2.00", TODAY.minusDays(8), false); // outside the window

        List<DailyRevenue> days = service.revenueByDay(TODAY);
        assertEquals(7, days.size());
        assertEquals(TODAY.minusDays(6), days.get(0).date());
        assertEquals(TODAY, days.get(6).date());
        assertEquals(new BigDecimal("4.00"), days.get(6).total());
        assertEquals(new BigDecimal("2.00"), days.get(3).total());
        assertEquals(BigDecimal.ZERO, days.get(0).total());
    }
}
