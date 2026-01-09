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

    /**
     * The Main Transaction Engine:
     * Saves the Sale Header, all Sale Items, and updates Batch Stock levels.
     */
    public boolean executeSale(Long customerId, DefaultTableModel cart, String paymentMethod, double totalAmount) {
        String insertSale = "INSERT INTO sales (customer_id, total_amount, payment_method, sale_date) VALUES (?, ?, ?, NOW())";
        String insertItem = "INSERT INTO sale_items (sale_id, item_id, batch_id, quantity, unit_price, subtotal) VALUES (?, ?, ?, ?, ?, ?)";
        String updateStock = "UPDATE batches SET quantity_remaining = quantity_remaining - ? WHERE batch_id = ? AND quantity_remaining >= ?";

        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false); // Enable ACID transaction

            long saleId = -1;
            // 1. Create Sale Header
            try (PreparedStatement ps1 = conn.prepareStatement(insertSale, Statement.RETURN_GENERATED_KEYS)) {
                if (customerId == null || customerId <= 0) {
                    ps1.setNull(1, Types.BIGINT);
                } else {
                    ps1.setLong(1, customerId);
                }
                ps1.setDouble(2, totalAmount);
                ps1.setString(3, paymentMethod);
                ps1.executeUpdate();

                ResultSet rs = ps1.getGeneratedKeys();
                if (rs.next()) saleId = rs.getLong(1);
            }

            if (saleId == -1) throw new SQLException("Failed to create sale header.");

            // 2. Process Cart and Stock
            try (PreparedStatement psItem = conn.prepareStatement(insertItem);
                 PreparedStatement psStock = conn.prepareStatement(updateStock)) {

                for (int i = 0; i < cart.getRowCount(); i++) {
                    int itemId = (int) cart.getValueAt(i, 0);
                    int batchId = (int) cart.getValueAt(i, 2);
                    int qty = Integer.parseInt(cart.getValueAt(i, 3).toString());
                    double unitPrice = (double) cart.getValueAt(i, 4);
                    double subtotal = (double) cart.getValueAt(i, 5);

                    // Add to Sale Items batch
                    psItem.setLong(1, saleId);
                    psItem.setInt(2, itemId);
                    psItem.setInt(3, batchId);
                    psItem.setInt(4, qty);
                    psItem.setDouble(5, unitPrice);
                    psItem.setDouble(6, subtotal);
                    psItem.addBatch();

                    // Add to Stock Reduction batch
                    psStock.setInt(1, qty);
                    psStock.setInt(2, batchId);
                    psStock.setInt(3, qty); // Check constraint: must have enough stock
                    psStock.addBatch();
                }

                psItem.executeBatch();
                int[] stockResults = psStock.executeBatch();

                // Verify all stock updates succeeded
                for (int res : stockResults) {
                    if (res == 0) throw new SQLException("Insufficient stock in one of the batches.");
                }
            }

            conn.commit(); // Finalize transaction
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    // --- DASHBOARD METHODS (Used by HomePanel) ---

    public double getTodaysProfit() {
        String sql = "SELECT SUM((si.unit_price - b.purchase_price) * si.quantity) " +
                "FROM sale_items si " +
                "JOIN sales s ON si.sale_id = s.sale_id " +
                "JOIN batches b ON si.batch_id = b.batch_id " +
                "WHERE DATE(s.sale_date) = CURDATE()";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
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
        } catch (SQLException e) { e.printStackTrace(); }
        return model;
    }
}