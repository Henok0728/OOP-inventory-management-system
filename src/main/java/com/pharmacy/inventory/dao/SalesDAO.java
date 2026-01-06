package com.pharmacy.inventory.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import javax.swing.table.DefaultTableModel;
import java.sql.*;

@Repository
public class SalesDAO {
    @Autowired
    private DataSource dataSource;

    // Fixed: Now joins sale_items to get quantity and price
    public double getTodaysProfit() {
        // Math: (Selling Price - Purchase Price) * Quantity
        String sql = "SELECT SUM((si.unit_price - b.purchase_price) * si.quantity) " +
                "FROM sale_items si " +
                "JOIN sales s ON si.sale_id = s.sale_id " +
                "JOIN batches b ON si.batch_id = b.batch_id " +
                "WHERE DATE(s.sale_date) = CURDATE()";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public int getTodaysSalesCount() {
        String sql = "SELECT SUM(quantity) FROM sale_items si " +
                "JOIN sales s ON si.sale_id = s.sale_id " +
                "WHERE DATE(s.sale_date) = CURDATE()";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) return rs.getInt(1);

        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public DefaultTableModel getTodaysSalesDetailsModel() {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Time", "Item Name", "Qty", "Total Price"}, 0);

        String sql = "SELECT TIME(s.sale_date) as time_only, i.name, si.quantity, si.subtotal " +
                "FROM sales s " +
                "JOIN sale_items si ON s.sale_id = si.sale_id " +
                "JOIN items i ON si.item_id = i.item_id " +
                "WHERE DATE(s.sale_date) = CURDATE() " +
                "ORDER BY s.sale_date DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("time_only"),
                        rs.getString("name"),
                        rs.getInt("quantity"),
                        "ETB " + String.format("%.2f", rs.getDouble("subtotal"))
                });
            }
        } catch (SQLException e) {
            System.err.println("Dashboard Error (Sales Detail): " + e.getMessage());

        }
        return model;
    }

    public boolean processSale(int customerId, int itemId, int batchId, int qty, double price, String method) {
        String insertSale = "INSERT INTO sales (customer_id, total_amount, payment_method) VALUES (?, ?, ?)";
        String insertItem = "INSERT INTO sale_items (sale_id, item_id, batch_id, quantity, unit_price, subtotal) VALUES (?, ?, ?, ?, ?, ?)";
        String updateStock = "UPDATE batches SET quantity_remaining = quantity_remaining - ? WHERE batch_id = ?";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // 1. Create Sale Header
                PreparedStatement ps1 = conn.prepareStatement(insertSale, Statement.RETURN_GENERATED_KEYS);

                // --- ADDED LOGIC HERE ---
                if (customerId <= 0) {
                    ps1.setNull(1, java.sql.Types.INTEGER); // Properly sends NULL to MySQL
                } else {
                    ps1.setInt(1, customerId);
                }
                // ------------------------

                ps1.setDouble(2, qty * price);
                ps1.setString(3, method);
                ps1.executeUpdate();

                ResultSet rs = ps1.getGeneratedKeys();
                if (!rs.next()) throw new SQLException("Generated Key failed");
                long saleId = rs.getLong(1);

                // 2. Create Sale Item
                PreparedStatement ps2 = conn.prepareStatement(insertItem);
                ps2.setLong(1, saleId);
                ps2.setInt(2, itemId);
                ps2.setInt(3, batchId);
                ps2.setInt(4, qty);
                ps2.setDouble(5, price);
                ps2.setDouble(6, qty * price);
                ps2.executeUpdate();

                // 3. Decrease Inventory
                PreparedStatement ps3 = conn.prepareStatement(updateStock);
                ps3.setInt(1, qty);
                ps3.setInt(2, batchId);
                ps3.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    public double getTodaysSalesRevenue() {
        String sql = "SELECT SUM(total_amount) FROM sales WHERE DATE(sale_date) = CURDATE()";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }
}