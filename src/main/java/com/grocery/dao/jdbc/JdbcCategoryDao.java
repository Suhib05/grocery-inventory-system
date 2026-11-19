package com.grocery.dao.jdbc;

import com.grocery.dao.CategoryDao;
import com.grocery.model.Category;

import javax.sql.DataSource;
import java.util.List;

public class JdbcCategoryDao implements CategoryDao {

    private final JdbcSupport jdbc;

    public JdbcCategoryDao(DataSource dataSource) { this.jdbc = new JdbcSupport(dataSource); }

    @Override
    public List<Category> findAll() {
        return jdbc.query("SELECT id, name, description FROM categories ORDER BY name",
                ps -> { },
                rs -> new Category(rs.getInt("id"), rs.getString("name"), rs.getString("description")));
    }

    @Override
    public boolean exists(int id) {
        return !jdbc.query("SELECT 1 FROM categories WHERE id = ?", ps -> ps.setInt(1, id), rs -> 1).isEmpty();
    }
}
