package com.grocery.support;

import com.grocery.dao.ProductDao;
import com.grocery.dao.ProductQuery;
import com.grocery.model.Product;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Fake ProductDao backed by a map, so services can be tested without a database. */
public class InMemoryProductDao implements ProductDao {

    private final Map<Integer, Product> products = new LinkedHashMap<>();
    private final InMemoryCategoryDao categories;
    private int nextId = 1;

    public InMemoryProductDao() { this(new InMemoryCategoryDao()); }

    public InMemoryProductDao(InMemoryCategoryDao categories) { this.categories = categories; }

    public InMemoryProductDao add(Product p) {
        products.put(p.id(), p);
        nextId = Math.max(nextId, p.id() + 1);
        return this;
    }

    @Override
    public List<Product> find(ProductQuery q) {
        List<Product> list = new ArrayList<>(products.values());
        if (q.keyword() != null) {
            list.removeIf(p -> !p.name().toLowerCase().contains(q.keyword().toLowerCase()));
        }
        if (q.categoryId() != null) list.removeIf(p -> p.categoryId() != q.categoryId());
        list.sort(Comparator.comparing(Product::name));
        return list;
    }

    @Override public Optional<Product> findById(int id) { return Optional.ofNullable(products.get(id)); }

    @Override
    public int insert(Product p) {
        int id = nextId++;
        products.put(id, withId(p, id));
        return id;
    }

    @Override
    public boolean update(Product p) {
        if (!products.containsKey(p.id())) return false;
        products.put(p.id(), withId(p, p.id()));
        return true;
    }

    @Override public boolean archive(int id) { return products.remove(id) != null; }

    /** Used by the fake SaleDao to change stock. */
    public void setQuantity(int id, int quantity) {
        Product p = products.get(id);
        products.put(id, new Product(p.id(), p.name(), p.categoryId(), p.categoryName(), quantity,
                p.price(), p.minStock(), p.expiryDate(), p.createdAt(), p.updatedAt()));
    }

    private Product withId(Product p, int id) {
        return new Product(id, p.name(), p.categoryId(), categories.nameOf(p.categoryId()), p.quantity(),
                p.price(), p.minStock(), p.expiryDate(), null, null);
    }
}
