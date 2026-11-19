package com.grocery.dao;

import com.grocery.model.Product;
import java.util.List;
import java.util.Optional;

/** Persistence for products. Archived (soft-deleted) products are never returned. */
public interface ProductDao {
    List<Product> find(ProductQuery query);
    Optional<Product> findById(int id);

    /** Inserts the product and returns its generated id. Id/timestamps on the argument are ignored. */
    int insert(Product product);

    /** Returns false if no active product has that id. */
    boolean update(Product product);

    /** Soft-deletes the product. Returns false if no active product has that id. */
    boolean archive(int id);
}
