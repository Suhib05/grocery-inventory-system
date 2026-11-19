package com.grocery.dao.jdbc;

import com.grocery.dao.ProductDao;
import com.grocery.dao.ProductQuery;
import com.grocery.model.Product;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcProductDao implements ProductDao {

    private static final String SELECT =
            "SELECT p.id, p.name, p.category_id, c.name AS category_name, p.quantity, p.price, "
          + "p.min_stock, p.expiry_date, p.created_at, p.updated_at "
          + "FROM products p JOIN categories c ON c.id = p.category_id "
          + "WHERE p.archived_at IS NULL ";

    private final JdbcSupport jdbc;

    public JdbcProductDao(DataSource dataSource) {
        this.jdbc = new JdbcSupport(dataSource);
    }

    @Override
    public List<Product> find(ProductQuery q) {
        StringBuilder sql = new StringBuilder(SELECT);
        List<Object> params = new ArrayList<>();
        if (q.keyword() != null && !q.keyword().isBlank()) {
            sql.append("AND p.name LIKE ? ESCAPE '!' ");
            params.add("%" + escapeLike(q.keyword().trim()) + "%");
        }
        if (q.categoryId() != null) {
            sql.append("AND p.category_id = ? ");
            params.add(q.categoryId());
        }
        // Column names come from an enum whitelist, never from user input.
        ProductQuery.SortField sort = q.sort() == null ? ProductQuery.SortField.NAME : q.sort();
        String dir = q.descending() ? "DESC" : "ASC";
        if (sort == ProductQuery.SortField.EXPIRY_DATE) {
            sql.append("ORDER BY p.expiry_date IS NULL, p.expiry_date ").append(dir); // no-expiry items last
        } else {
            sql.append("ORDER BY ").append(sort.column()).append(' ').append(dir);
        }
        sql.append(", p.id");

        return jdbc.query(sql.toString(), ps -> {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
        }, JdbcProductDao::map);
    }

    @Override
    public Optional<Product> findById(int id) {
        return jdbc.query(SELECT + "AND p.id = ?", ps -> ps.setInt(1, id), JdbcProductDao::map)
                .stream().findFirst();
    }

    @Override
    public int insert(Product p) {
        String sql = "INSERT INTO products (name, category_id, quantity, price, min_stock, expiry_date) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        return jdbc.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(ps, p);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No id returned for new product");
                    return keys.getInt(1);
                }
            }
        });
    }

    @Override
    public boolean update(Product p) {
        String sql = "UPDATE products SET name = ?, category_id = ?, quantity = ?, price = ?, "
                   + "min_stock = ?, expiry_date = ? WHERE id = ? AND archived_at IS NULL";
        return jdbc.update(sql, ps -> { bind(ps, p); ps.setInt(7, p.id()); }) > 0;
    }

    @Override
    public boolean archive(int id) {
        return jdbc.update("UPDATE products SET archived_at = CURRENT_TIMESTAMP WHERE id = ? AND archived_at IS NULL",
                ps -> ps.setInt(1, id)) > 0;
    }


    private static void bind(PreparedStatement ps, Product p) throws SQLException {
        ps.setString(1, p.name());
        ps.setInt(2, p.categoryId());
        ps.setInt(3, p.quantity());
        ps.setBigDecimal(4, p.price());
        ps.setInt(5, p.minStock());
        if (p.expiryDate() != null) ps.setObject(6, p.expiryDate());
        else                        ps.setNull(6, Types.DATE);
    }

    static Product map(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("category_id"),
                rs.getString("category_name"),
                rs.getInt("quantity"),
                rs.getBigDecimal("price"),
                rs.getInt("min_stock"),
                rs.getObject("expiry_date", LocalDate.class),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("updated_at", LocalDateTime.class));
    }

    /** Escapes LIKE wildcards so a search for "50%" matches the literal text. */
    static String escapeLike(String s) {
        return s.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
