package com.grocery.support;

import com.grocery.dao.InsufficientStockException;
import com.grocery.dao.SaleDao;
import com.grocery.model.DailyRevenue;
import com.grocery.model.Product;
import com.grocery.model.Sale;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class InMemorySaleDao implements SaleDao {

    private final InMemoryProductDao products;
    private final List<Sale> sales = new ArrayList<>();
    /** Simulates another cashier selling stock between the service's check and the update. */
    public boolean simulateRace = false;

    public InMemorySaleDao(InMemoryProductDao products) { this.products = products; }

    public void addSale(int productId, int qty, String unitPrice, LocalDate date, boolean voided) {
        BigDecimal price = new BigDecimal(unitPrice);
        String name = products.findById(productId).map(Product::name).orElse("P" + productId);
        sales.add(new Sale(sales.size() + 1, productId, name, qty, price,
                price.multiply(BigDecimal.valueOf(qty)), date, "Sam Rivera",
                voided ? LocalDateTime.now() : null, null));
    }

    @Override
    public List<Sale> findBetween(LocalDate from, LocalDate to) {
        return sales.stream()
                .filter(s -> !s.saleDate().isBefore(from) && !s.saleDate().isAfter(to))
                .sorted(Comparator.comparing(Sale::saleDate).reversed())
                .toList();
    }

    @Override
    public Optional<Sale> findById(int id) {
        return sales.stream().filter(s -> s.id() == id).findFirst();
    }

    @Override
    public int recordSale(int productId, int quantity, BigDecimal unitPrice, LocalDate saleDate, int userId) {
        Product p = products.findById(productId).orElseThrow();
        if (simulateRace || p.quantity() < quantity) throw new InsufficientStockException("Not enough stock left.");
        products.setQuantity(productId, p.quantity() - quantity);
        int id = sales.size() + 1;
        sales.add(new Sale(id, productId, p.name(), quantity, unitPrice,
                unitPrice.multiply(BigDecimal.valueOf(quantity)), saleDate, "Sam Rivera", null, null));
        return id;
    }

    @Override
    public boolean voidSale(int saleId, int userId) {
        for (int i = 0; i < sales.size(); i++) {
            Sale s = sales.get(i);
            if (s.id() == saleId && !s.isVoided()) {
                sales.set(i, new Sale(s.id(), s.productId(), s.productName(), s.quantitySold(), s.unitPrice(),
                        s.totalAmount(), s.saleDate(), s.recordedByName(), LocalDateTime.now(), s.createdAt()));
                products.findById(s.productId()).ifPresent(p ->
                        products.setQuantity(p.id(), p.quantity() + s.quantitySold()));
                return true;
            }
        }
        return false;
    }

    @Override
    public BigDecimal revenueBetween(LocalDate from, LocalDate to) {
        return findBetween(from, to).stream().filter(s -> !s.isVoided())
                .map(Sale::totalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public List<DailyRevenue> dailyRevenue(LocalDate from, LocalDate to) {
        Map<LocalDate, BigDecimal> byDay = new TreeMap<>();
        findBetween(from, to).stream().filter(s -> !s.isVoided())
                .forEach(s -> byDay.merge(s.saleDate(), s.totalAmount(), BigDecimal::add));
        return byDay.entrySet().stream().map(e -> new DailyRevenue(e.getKey(), e.getValue())).toList();
    }
}
