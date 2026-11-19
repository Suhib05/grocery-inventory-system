package com.grocery.support;

import com.grocery.dao.CategoryDao;
import com.grocery.model.Category;

import java.util.List;

public class InMemoryCategoryDao implements CategoryDao {

    private final List<Category> categories;

    public InMemoryCategoryDao() {
        this(List.of(new Category(1, "Dairy & Eggs", null), new Category(2, "Bakery", null)));
    }

    public InMemoryCategoryDao(List<Category> categories) { this.categories = categories; }

    @Override public List<Category> findAll() { return categories; }

    @Override public boolean exists(int id) { return categories.stream().anyMatch(c -> c.id() == id); }

    public String nameOf(int id) {
        return categories.stream().filter(c -> c.id() == id).map(Category::name).findFirst().orElse(null);
    }
}
