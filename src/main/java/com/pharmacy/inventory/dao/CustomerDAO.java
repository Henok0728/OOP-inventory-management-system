package com.pharmacy.inventory.dao;

import com.pharmacy.inventory.model.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CustomerDAO {

    @Autowired
    private DataSource dataSource;

    /**
     * Fetches all registered customers for the selection dropdown.
     */
    public List<Customer> getAllCustomers() {
        List<Customer> customers = new ArrayList<>();
        String sql = "SELECT * FROM customers ORDER BY first_name ASC";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Customer c = new Customer();
                c.setCustomerId(rs.getLong("customer_id"));
                c.setFirstName(rs.getString("first_name"));
                c.setLastName(rs.getString("last_name"));
                c.setMedicalRecordNumber(rs.getString("medical_record_number"));
                customers.add(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return customers;
    }

    /**
     * Missing Method: Saves a new customer during the 'Quick Add' process.
     */
    public boolean saveCustomer(Customer c) {
        String sql = "INSERT INTO customers (first_name, last_name, medical_record_number, created_at) VALUES (?, ?, ?, NOW())";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, c.getFirstName());
            ps.setString(2, c.getLastName());
            ps.setString(3, c.getMedicalRecordNumber());

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                // Fetch the new ID and set it back to the object
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        c.setCustomerId(rs.getLong(1));
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error saving customer: " + e.getMessage());
            return false;
        }
    }
}