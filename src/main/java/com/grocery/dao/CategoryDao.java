package com.grocery.dao;

import com.grocery.model.Category;
import java.util.List;

public interface CategoryDao {
    List<Category> findAll();
    boolean exists(int id);
}
