package com.pharmacy.inventory.dao;

import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import javax.swing.table.DefaultTableModel;

@Repository
public class WasteDAO {
    private final DataSource dataSource;

    public WasteDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // Find products that are expired but still in the "batches" table
    public List<Map<String, Object>> getExpiredBatches() {
        List<Map<String, Object>> list = new ArrayList<>();
        // Added i.item_id to the SELECT statement
        String sql = "SELECT b.batch_id, i.item_id, i.name, b.batch_number, b.quantity_remaining, b.expiration_date " +
                "FROM batches b JOIN items i ON b.item_id = i.item_id " +
                "WHERE b.expiration_date <= CURDATE() AND b.quantity_remaining > 0";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getInt("batch_id"));
                map.put("item_id", rs.getInt("item_id")); // Store the item_id for the NotificationManager
                map.put("name", rs.getString("name"));
                map.put("qty", rs.getInt("quantity_remaining"));
                map.put("expiry", rs.getDate("expiration_date"));
                list.add(map);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
    // Move a batch to waste and set quantity to 0 in batches table
    public void moveToWaste(int batchId, int itemId, int qty, String reason, int userId) {
        // Ensure 'reason' passed here is 'expired', 'damaged', or 'recalled'
        String sql = "INSERT INTO waste (batch_id, item_id, quantity_removed, reason, processed_by) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, batchId);
            ps.setInt(2, itemId);
            ps.setInt(3, qty);
            ps.setString(4, reason);
            ps.setInt(5, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            // This will now catch and print if a constraint is still violated
            e.printStackTrace();
        }
    }
    public DefaultTableModel getWasteLogs() {
        Vector<String> columns = new Vector<>();
        columns.add("Waste ID");
        columns.add("Product Name");
        columns.add("Batch #");
        columns.add("Qty Removed");
        columns.add("Reason");
        columns.add("Processed By");
        columns.add("Date");

        Vector<Vector<Object>> data = new Vector<>();

        // Using your exact column names: quantity_removed, recorded_at, item_id
        String sql = "SELECT w.waste_id, i.name as item_name, b.batch_number, w.quantity_removed, " +
                "w.reason, u.name as staff_name, w.recorded_at " +
                "FROM waste w " +
                "JOIN items i ON w.item_id = i.item_id " +
                "JOIN batches b ON w.batch_id = b.batch_id " +
                "JOIN users u ON w.processed_by = u.user_id " +
                "ORDER BY w.recorded_at DESC";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getLong("waste_id"));
                row.add(rs.getString("item_name"));
                row.add(rs.getString("batch_number"));
                row.add(rs.getInt("quantity_removed")); // Corrected column name
                row.add(rs.getString("reason"));
                row.add(rs.getString("staff_name"));
                row.add(rs.getTimestamp("recorded_at")); // Corrected column name
                data.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new DefaultTableModel(data, columns);
    }

    public void removeStockFromBatch(int batchId) {
        // We set remaining to 0 because the entire batch is being discarded as waste
        String sql = "UPDATE batches SET quantity_remaining = 0, status = 'expired' WHERE batch_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, batchId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}