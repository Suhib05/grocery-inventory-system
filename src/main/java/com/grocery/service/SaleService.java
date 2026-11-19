package com.grocery.service;

import com.grocery.dao.InsufficientStockException;
import com.grocery.dao.ProductDao;
import com.grocery.dao.SaleDao;
import com.grocery.model.Product;
import com.grocery.model.Sale;
import com.grocery.model.User;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Business rules for recording, listing and voiding sales. */
public class SaleService {

    public static final int MAX_QUANTITY_PER_SALE = 10_000;
    public static final int MAX_REPORT_DAYS = 366;

    public record SalesReport(LocalDate from, LocalDate to, List<Sale> sales,
                              BigDecimal revenue, int unitsSold, int transactions) { }

    private final SaleDao saleDao;
    private final ProductDao productDao;
    private final Clock clock;

    public SaleService(SaleDao saleDao, ProductDao productDao, Clock clock) {
        this.saleDao = saleDao;
        this.productDao = productDao;
        this.clock = clock;
    }

    public SalesReport report(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) throw new ValidationException("The start date must be on or before the end date.");
        if (ChronoUnit.DAYS.between(from, to) >= MAX_REPORT_DAYS) {
            throw new ValidationException("Please choose a range of at most " + MAX_REPORT_DAYS + " days.");
        }
        List<Sale> sales = saleDao.findBetween(from, to);
        List<Sale> counted = sales.stream().filter(s -> !s.isVoided()).toList();
        BigDecimal revenue = counted.stream().map(Sale::totalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        int units = counted.stream().mapToInt(Sale::quantitySold).sum();
        return new SalesReport(from, to, sales, revenue, units, counted.size());
    }

    public Sale record(SaleInput in, User user) {
        if (in == null) throw new ValidationException("Sale details are required.");
        LocalDate today = LocalDate.now(clock);
        Map<String, String> errors = new LinkedHashMap<>();

        if (in.productId() == null) errors.put("productId", "Please choose a product.");
        if (in.quantity() == null || in.quantity() < 1) errors.put("quantity", "Quantity must be at least 1.");
        else if (in.quantity() > MAX_QUANTITY_PER_SALE) errors.put("quantity", "Quantity is too large for one sale.");
        LocalDate saleDate = in.saleDate() == null ? today : in.saleDate();
        if (saleDate.isAfter(today)) errors.put("saleDate", "A sale can't be dated in the future.");
        if (!errors.isEmpty()) throw new ValidationException("Please fix the highlighted fields.", errors);

        Product product = productDao.findById(in.productId())
                .orElseThrow(() -> new NotFoundException("Product " + in.productId() + " was not found."));
        if (product.isExpiredOn(today)) {
            throw new ConflictException("\"" + product.name() + "\" has expired and can't be sold.");
        }
        if (product.quantity() < in.quantity()) {
            throw new ConflictException("Only " + product.quantity() + " unit(s) of \"" + product.name() + "\" left in stock.");
        }

        try {
            int id = saleDao.recordSale(product.id(), in.quantity(), product.price(), saleDate, user.id());
            return saleDao.findById(id).orElseThrow();
        } catch (InsufficientStockException e) {
            // Someone else sold the stock between our check and the update.
            throw new ConflictException(e.getMessage());
        }
    }

    public void voidSale(int saleId, User user) {
        Sale sale = saleDao.findById(saleId)
                .orElseThrow(() -> new NotFoundException("Sale " + saleId + " was not found."));
        if (sale.isVoided() || !saleDao.voidSale(saleId, user.id())) {
            throw new ConflictException("This sale has already been voided.");
        }
    }
}
