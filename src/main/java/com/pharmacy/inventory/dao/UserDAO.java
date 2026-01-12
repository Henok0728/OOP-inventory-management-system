package com.pharmacy.inventory.dao;

import com.pharmacy.inventory.model.User;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.*;

@Repository
public class UserDAO {
    private final DataSource dataSource;

    public UserDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Verifies user credentials and returns a User object if successful.
     */
    public User authenticate(String email, String password) {
        // In a real app, use: SELECT * FROM users WHERE email = ?
        // Then verify the password_hash using BCrypt.checkpw()
        String sql = "SELECT user_id, name, email, role FROM users WHERE email = ? AND password_hash = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setString(2, password); // For testing, we assume plain text or pre-hashed match

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setUserId(rs.getInt("user_id"));
                    user.setName(rs.getString("name"));
                    user.setEmail(rs.getString("email"));
                    user.setRole(rs.getString("role"));
                    return user;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Return null if authentication fails
    }
}