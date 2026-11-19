package com.grocery.dao.jdbc;

import com.grocery.dao.DataAccessException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Small helper that removes JDBC boilerplate and always closes resources. */
final class JdbcSupport {

    @FunctionalInterface interface Binder    { void bind(PreparedStatement ps) throws SQLException; }
    @FunctionalInterface interface RowMapper<T> { T map(ResultSet rs) throws SQLException; }
    @FunctionalInterface interface TxWork<T>  { T run(Connection c) throws SQLException; }

    private final DataSource dataSource;

    JdbcSupport(DataSource dataSource) { this.dataSource = dataSource; }

    <T> List<T> query(String sql, Binder binder, RowMapper<T> mapper) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (rs.next()) rows.add(mapper.map(rs));
                return rows;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Query failed", e);
        }
    }

    int update(String sql, Binder binder) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Update failed", e);
        }
    }

    /** Runs work in a transaction: commits on success, rolls back on any exception. */
    <T> T inTransaction(TxWork<T> work) {
        try (Connection c = dataSource.getConnection()) {
            boolean oldAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(oldAutoCommit);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Transaction failed", e);
        }
    }
}
