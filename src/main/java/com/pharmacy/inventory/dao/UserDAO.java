package com.pharmacy.inventory.dao;

import com.pharmacy.inventory.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserDAO {

    private final JdbcTemplate jdbcTemplate;

    // Spring will automatically inject the JdbcTemplate here
    @Autowired
    public UserDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // RowMapper tells Spring how to turn a database row into a User object
    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password_hash"));
        user.setRole(rs.getString("role"));
        // Make sure your User model has this field!
        user.setInWarehouse(rs.getBoolean("in_warehouse"));
        return user;
    };

    public User authenticate(String email, String password) {
        String sql = "SELECT * FROM users WHERE email = ? AND password_hash = ?";
        try {
            return jdbcTemplate.queryForObject(sql, userRowMapper, email, password);
        } catch (Exception e) {
            return null; // No user found or wrong password
        }
    }

    public List<User> getAllUsers() {
        String sql = "SELECT * FROM users";
        return jdbcTemplate.query(sql, userRowMapper);
    }

    public boolean addUser(User user) {
        String sql = "INSERT INTO users (name, email, password_hash, role) VALUES (?, ?, ?, ?)";
        int rows = jdbcTemplate.update(sql, user.getName(), user.getEmail(), user.getPassword(), user.getRole());
        return rows > 0;
    }

    public boolean deleteUser(long userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        return jdbcTemplate.update(sql, userId) > 0;
    }

    public User findByRfidTag(String tag) {
        String sql = "SELECT * FROM users WHERE rfid_tag = ?";
        try {
            // Using the userRowMapper defined above
            return jdbcTemplate.queryForObject(sql, userRowMapper, tag);
        } catch (Exception e) {
            return null; // Tag not registered or error
        }
    }

    public void updateWarehouseStatus(long userId, boolean inWarehouse) {
        String sql = "UPDATE users SET in_warehouse = ? WHERE user_id = ?";
        jdbcTemplate.update(sql, inWarehouse, userId);
    }
}