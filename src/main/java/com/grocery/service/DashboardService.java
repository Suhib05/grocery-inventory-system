package com.grocery.service;

import com.grocery.dao.CategoryDao;
import com.grocery.dao.ProductDao;
import com.grocery.dao.ProductQuery;
import com.grocery.dao.SaleDao;
import com.grocery.model.DailyRevenue;
import com.grocery.model.Product;
import com.grocery.model.ProductStatus;
import com.grocery.model.ProductView;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Builds the numbers and short lists shown on the dashboard. */
public class DashboardService {

    static final int LIST_LIMIT = 8;
    static final int CHART_DAYS = 7;

    public record Summary(
            int totalProducts,
            int categories,
            BigDecimal stockValue,
            Map<ProductStatus, Integer> statusCounts,
            BigDecimal revenueToday,
            BigDecimal revenueLast7Days,
            BigDecimal revenueLast30Days,
            List<DailyRevenue> revenueByDay,
            List<ProductView> lowStock,
            List<ProductView> expiringSoon,
            List<ProductView> expired) { }

    private final ProductDao productDao;
    private final CategoryDao categoryDao;
    private final SaleDao saleDao;
    private final Clock clock;

    public DashboardService(ProductDao productDao, CategoryDao categoryDao, SaleDao saleDao, Clock clock) {
        this.productDao = productDao;
        this.categoryDao = categoryDao;
        this.saleDao = saleDao;
        this.clock = clock;
    }

    public Summary summary() {
        LocalDate today = LocalDate.now(clock);
        List<Product> products = productDao.find(ProductQuery.all());
        List<ProductView> views = products.stream().map(p -> ProductView.of(p, today)).toList();

        Map<ProductStatus, Integer> counts = new EnumMap<>(ProductStatus.class);
        for (ProductStatus s : ProductStatus.values()) counts.put(s, 0);
        views.forEach(v -> counts.merge(v.status(), 1, Integer::sum));

        BigDecimal stockValue = products.stream().map(Product::stockValue).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ProductView> lowStock = views.stream()
                .filter(v -> v.status() == ProductStatus.LOW_STOCK || v.status() == ProductStatus.OUT_OF_STOCK)
                .sorted(Comparator.comparingInt(ProductView::quantity))
                .limit(LIST_LIMIT).toList();
        List<ProductView> expiringSoon = byExpiry(views, ProductStatus.EXPIRING_SOON);
        List<ProductView> expired = byExpiry(views, ProductStatus.EXPIRED);

        return new Summary(
                products.size(),
                categoryDao.findAll().size(),
                stockValue,
                counts,
                saleDao.revenueBetween(today, today),
                saleDao.revenueBetween(today.minusDays(6), today),
                saleDao.revenueBetween(today.minusDays(29), today),
                revenueByDay(today),
                lowStock, expiringSoon, expired);
    }

    /** Revenue for each of the last 7 days, with 0 for days without sales so the chart has no gaps. */
    List<DailyRevenue> revenueByDay(LocalDate today) {
        LocalDate from = today.minusDays(CHART_DAYS - 1L);
        Map<LocalDate, BigDecimal> byDate = saleDao.dailyRevenue(from, today).stream()
                .collect(Collectors.toMap(DailyRevenue::date, DailyRevenue::total));
        List<DailyRevenue> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
            days.add(new DailyRevenue(d, byDate.getOrDefault(d, BigDecimal.ZERO)));
        }
        return days;
    }

    private static List<ProductView> byExpiry(List<ProductView> views, ProductStatus status) {
        return views.stream()
                .filter(v -> v.status() == status)
                .sorted(Comparator.comparing(ProductView::expiryDate))
                .limit(LIST_LIMIT).toList();
    }

}
