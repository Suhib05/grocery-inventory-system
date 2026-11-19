package com.grocery.service;

import com.grocery.dao.CategoryDao;
import com.grocery.dao.ProductDao;
import com.grocery.dao.ProductQuery;
import com.grocery.model.Category;
import com.grocery.model.Product;
import com.grocery.model.ProductStatus;
import com.grocery.model.ProductView;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Business rules for products: validation, derived status and expiry alerts. */
public class ProductService {

    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_QUANTITY = 1_000_000;
    public static final BigDecimal MAX_PRICE = new BigDecimal("99999999.99");
    public static final int DEFAULT_MIN_STOCK = 10;

    public record ExpiryAlerts(int days, List<ProductView> expired, List<ProductView> expiringSoon) { }

    private final ProductDao productDao;
    private final CategoryDao categoryDao;
    private final Clock clock;

    public ProductService(ProductDao productDao, CategoryDao categoryDao, Clock clock) {
        this.productDao = productDao;
        this.categoryDao = categoryDao;
        this.clock = clock;
    }

    public List<Category> categories() { return categoryDao.findAll(); }

    /** Lists products; {@code status} is optional and filters on the derived status. */
    public List<ProductView> list(ProductQuery query, ProductStatus status) {
        LocalDate today = today();
        return productDao.find(query).stream()
                .map(p -> ProductView.of(p, today))
                .filter(v -> status == null || v.status() == status)
                .toList();
    }

    public ProductView get(int id) {
        return ProductView.of(find(id), today());
    }

    public ProductView create(ProductInput input) {
        Product valid = validate(0, input);
        int id = productDao.insert(valid);
        return get(id);
    }

    public ProductView update(int id, ProductInput input) {
        find(id); // 404 if it doesn't exist
        Product valid = validate(id, input);
        if (!productDao.update(valid)) throw notFound(id);
        return get(id);
    }

    public void archive(int id) {
        if (!productDao.archive(id)) throw notFound(id);
    }

    /** Expired products, and products expiring within {@code days} days. */
    public ExpiryAlerts expiryAlerts(int days) {
        if (days < 1 || days > 90) throw new ValidationException("Days must be between 1 and 90.");
        LocalDate today = today();
        List<Product> all = productDao.find(ProductQuery.all());
        Comparator<ProductView> byExpiry = Comparator.comparing(ProductView::expiryDate);
        List<ProductView> expired = all.stream()
                .filter(p -> p.quantity() > 0 && p.isExpiredOn(today))
                .map(p -> ProductView.of(p, today)).sorted(byExpiry).toList();
        List<ProductView> soon = all.stream()
                .filter(p -> p.quantity() > 0 && p.isExpiringSoonOn(today, days))
                .map(p -> ProductView.of(p, today)).sorted(byExpiry).toList();
        return new ExpiryAlerts(days, expired, soon);
    }

    Product find(int id) {
        return productDao.findById(id).orElseThrow(() -> notFound(id));
    }

    private Product validate(int id, ProductInput in) {
        if (in == null) throw new ValidationException("Product details are required.");
        Map<String, String> errors = new LinkedHashMap<>();

        String name = in.name() == null ? "" : in.name().trim().replaceAll("\\s+", " ");
        if (name.isEmpty())                       errors.put("name", "Product name is required.");
        else if (name.length() > MAX_NAME_LENGTH) errors.put("name", "Name must be at most " + MAX_NAME_LENGTH + " characters.");

        if (in.categoryId() == null)                  errors.put("categoryId", "Please choose a category.");
        else if (!categoryDao.exists(in.categoryId())) errors.put("categoryId", "That category doesn't exist.");

        if (in.quantity() == null)          errors.put("quantity", "Quantity is required.");
        else if (in.quantity() < 0)         errors.put("quantity", "Quantity can't be negative.");
        else if (in.quantity() > MAX_QUANTITY) errors.put("quantity", "Quantity is too large.");

        BigDecimal price = in.price();
        if (price == null)                               errors.put("price", "Price is required.");
        else if (price.signum() < 0)                     errors.put("price", "Price can't be negative.");
        else if (price.compareTo(MAX_PRICE) > 0)         errors.put("price", "Price is too large.");
        else if (price.stripTrailingZeros().scale() > 2) errors.put("price", "Price can have at most 2 decimal places.");

        int minStock = in.minStock() == null ? DEFAULT_MIN_STOCK : in.minStock();
        if (minStock < 0)                 errors.put("minStock", "Minimum stock can't be negative.");
        else if (minStock > MAX_QUANTITY) errors.put("minStock", "Minimum stock is too large.");

        if (!errors.isEmpty()) throw new ValidationException("Please fix the highlighted fields.", errors);

        return new Product(id, name, in.categoryId(), null, in.quantity(),
                price.setScale(2), minStock, in.expiryDate(), null, null);
    }

    private LocalDate today() { return LocalDate.now(clock); }

    private static NotFoundException notFound(int id) {
        return new NotFoundException("Product " + id + " was not found.");
    }
}
