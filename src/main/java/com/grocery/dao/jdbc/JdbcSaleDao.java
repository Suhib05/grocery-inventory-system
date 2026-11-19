package com.grocery.dao.jdbc;

import com.grocery.dao.InsufficientStockException;
import com.grocery.dao.SaleDao;
import com.grocery.model.DailyRevenue;
import com.grocery.model.Sale;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class JdbcSaleDao implements SaleDao {

    private static final String SELECT =
            "SELECT s.id, s.product_id, p.name AS product_name, s.quantity_sold, s.unit_price, "
          + "s.total_amount, s.sale_date, u.full_name AS recorded_by_name, s.voided_at, s.created_at "
          + "FROM sales s "
          + "JOIN products p ON p.id = s.product_id "
          + "LEFT JOIN users u ON u.id = s.recorded_by ";

    private final JdbcSupport jdbc;

    public JdbcSaleDao(DataSource dataSource) { this.jdbc = new JdbcSupport(dataSource); }

    @Override
    public List<Sale> findBetween(LocalDate from, LocalDate to) {
        return jdbc.query(SELECT + "WHERE s.sale_date BETWEEN ? AND ? ORDER BY s.sale_date DESC, s.id DESC",
                ps -> { ps.setObject(1, from); ps.setObject(2, to); },
                JdbcSaleDao::map);
    }

    @Override
    public Optional<Sale> findById(int id) {
        return jdbc.query(SELECT + "WHERE s.id = ?", ps -> ps.setInt(1, id), JdbcSaleDao::map)
                .stream().findFirst();
    }

    @Override
    public int recordSale(int productId, int quantity, BigDecimal unitPrice, LocalDate saleDate, int userId) {
        return jdbc.inTransaction(c -> {
            // Conditional update: only succeeds if enough stock is left right now.
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE products SET quantity = quantity - ? "
                  + "WHERE id = ? AND archived_at IS NULL AND quantity >= ?")) {
                ps.setInt(1, quantity);
                ps.setInt(2, productId);
                ps.setInt(3, quantity);
                if (ps.executeUpdate() == 0) {
                    throw new InsufficientStockException("Not enough stock left to sell " + quantity + " unit(s).");
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO sales (product_id, quantity_sold, unit_price, sale_date, recorded_by) "
                  + "VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, productId);
                ps.setInt(2, quantity);
                ps.setBigDecimal(3, unitPrice);
                ps.setObject(4, saleDate);
                ps.setInt(5, userId);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No id returned for new sale");
                    return keys.getInt(1);
                }
            }
        });
    }

    @Override
    public boolean voidSale(int saleId, int userId) {
        return jdbc.inTransaction(c -> {
            int productId;
            int quantity;
            // Lock the sale row so two people can't void it at the same time.
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT product_id, quantity_sold FROM sales WHERE id = ? AND voided_at IS NULL FOR UPDATE")) {
                ps.setInt(1, saleId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return false;
                    productId = rs.getInt(1);
                    quantity = rs.getInt(2);
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE sales SET voided_at = CURRENT_TIMESTAMP, voided_by = ? WHERE id = ?")) {
                ps.setInt(1, userId);
                ps.setInt(2, saleId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE products SET quantity = quantity + ? WHERE id = ?")) {
                ps.setInt(1, quantity);
                ps.setInt(2, productId);
                ps.executeUpdate();
            }
            return true;
        });
    }

    @Override
    public BigDecimal revenueBetween(LocalDate from, LocalDate to) {
        List<BigDecimal> r = jdbc.query(
                "SELECT COALESCE(SUM(total_amount), 0) FROM sales "
              + "WHERE voided_at IS NULL AND sale_date BETWEEN ? AND ?",
                ps -> { ps.setObject(1, from); ps.setObject(2, to); },
                rs -> rs.getBigDecimal(1));
        return r.isEmpty() || r.get(0) == null ? BigDecimal.ZERO : r.get(0);
    }

    @Override
    public List<DailyRevenue> dailyRevenue(LocalDate from, LocalDate to) {
        return jdbc.query(
                "SELECT sale_date, SUM(total_amount) AS total FROM sales "
              + "WHERE voided_at IS NULL AND sale_date BETWEEN ? AND ? "
              + "GROUP BY sale_date ORDER BY sale_date",
                ps -> { ps.setObject(1, from); ps.setObject(2, to); },
                rs -> new DailyRevenue(rs.getObject("sale_date", LocalDate.class), rs.getBigDecimal("total")));
    }

    private static Sale map(ResultSet rs) throws SQLException {
        return new Sale(
                rs.getInt("id"),
                rs.getInt("product_id"),
                rs.getString("product_name"),
                rs.getInt("quantity_sold"),
                rs.getBigDecimal("unit_price"),
                rs.getBigDecimal("total_amount"),
                rs.getObject("sale_date", LocalDate.class),
                rs.getString("recorded_by_name"),
                rs.getObject("voided_at", LocalDateTime.class),
                rs.getObject("created_at", LocalDateTime.class));
    }
}
