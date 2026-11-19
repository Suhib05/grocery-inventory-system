package com.grocery.dao.jdbc;

import com.grocery.dao.UserDao;
import com.grocery.model.Role;
import com.grocery.model.User;
import com.grocery.model.UserCredentials;

import javax.sql.DataSource;
import java.util.Optional;

public class JdbcUserDao implements UserDao {

    private final JdbcSupport jdbc;

    public JdbcUserDao(DataSource dataSource) { this.jdbc = new JdbcSupport(dataSource); }

    @Override
    public Optional<UserCredentials> findByUsername(String username) {
        return jdbc.query(
                "SELECT id, username, full_name, role, password_hash, active FROM users WHERE username = ?",
                ps -> ps.setString(1, username),
                rs -> new UserCredentials(
                        new User(rs.getInt("id"), rs.getString("username"), rs.getString("full_name"),
                                Role.valueOf(rs.getString("role"))),
                        rs.getString("password_hash"),
                        rs.getBoolean("active")))
                .stream().findFirst();
    }

    @Override
    public void updateLastLogin(int userId) {
        jdbc.update("UPDATE users SET last_login_at = CURRENT_TIMESTAMP WHERE id = ?", ps -> ps.setInt(1, userId));
    }
}
