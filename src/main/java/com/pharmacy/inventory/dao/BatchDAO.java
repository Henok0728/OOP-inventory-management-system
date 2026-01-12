package com.pharmacy.inventory.dao;

import com.pharmacy.inventory.model.Batch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

@Repository
public class BatchDAO {

    @Autowired
    private DataSource dataSource;

    public boolean addBatch(Batch batch) {
        String sql = "INSERT INTO batches (batch_number, item_id, quantity_received, quantity_remaining, " +
                "manufactured_date, expiration_date, purchase_price, selling_price, " +
                "storage_location, status, received_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURDATE())";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, batch.getBatchNumber());
            pstmt.setInt(2, batch.getItemId());
            pstmt.setInt(3, batch.getQuantityReceived());
            pstmt.setInt(4, batch.getQuantityReceived()); // quantity_remaining starts at received amount
            pstmt.setString(5, batch.getManufacturedDate()); // matches your schema
            pstmt.setString(6, batch.getExpirationDate());
            pstmt.setDouble(7, batch.getPurchasePrice());
            pstmt.setDouble(8, batch.getSellingPrice());
            pstmt.setString(9, batch.getStorageLocation()); // matches your schema
            pstmt.setString(10, batch.getStatus() == null ? "active" : batch.getStatus());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding batch: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deducts stock when a sale occurs.
     */
    public boolean updateStockQuantity(long batchId, int quantitySold) {
        String sql = "UPDATE batches SET quantity_remaining = quantity_remaining - ? " +
                "WHERE batch_id = ? AND quantity_remaining >= ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, quantitySold);
            pstmt.setLong(2, batchId);
            pstmt.setInt(3, quantitySold);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public DefaultTableModel getBatchesByItemIdModel(int itemId) {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"ID", "Batch #", "Remaining", "Expiry", "Price", "Location", "Status"}, 0);

        String sql = "SELECT batch_id, batch_number, quantity_remaining, expiration_date, " +
                "selling_price, storage_location, status " +
                "FROM batches WHERE item_id = ? ORDER BY expiration_date ASC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getLong("batch_id"),
                        rs.getString("batch_number"),
                        rs.getInt("quantity_remaining"),
                        rs.getString("expiration_date"),
                        "ETB " + String.format("%.2f", rs.getDouble("selling_price")),
                        rs.getString("storage_location"),
                        rs.getString("status")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return model;
    }

    public DefaultTableModel getLowStockBatchesModel() {
        DefaultTableModel model = new DefaultTableModel(new String[]{"Item Name", "Batch #", "Qty Left"}, 0);
        String sql = "SELECT i.name, b.batch_number, b.quantity_remaining " +
                "FROM batches b JOIN items i ON b.item_id = i.item_id " +
                "WHERE b.quantity_remaining < 10 AND b.status = 'active'";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("name"),
                        rs.getString("batch_number"),
                        rs.getInt("quantity_remaining")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return model;
    }

    public DefaultTableModel getExpiringBatchesModel() {
        DefaultTableModel model = new DefaultTableModel(new String[]{"Item Name", "Batch #", "Expiry Date"}, 0);
        String sql = "SELECT i.name, b.batch_number, b.expiration_date " +
                "FROM batches b JOIN items i ON b.item_id = i.item_id " +
                "WHERE b.expiration_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY) " +
                "AND b.expiration_date >= CURDATE() AND b.status = 'active'";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("name"),
                        rs.getString("batch_number"),
                        rs.getString("expiration_date")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return model;
    }

    public DefaultTableModel getBatchesByItem(int itemId) {
        Vector<String> columns = new Vector<>();
        columns.add("Batch ID");
        columns.add("Batch Number");
        columns.add("Qty Remaining");
        columns.add("Cost Price");
        columns.add("Expiry Date");
        columns.add("Supplier");

        Vector<Vector<Object>> data = new Vector<>();
        // SQL to join batches with suppliers to show the name instead of just ID
        String sql = "SELECT b.batch_id, b.batch_number, b.quantity_remaining, b.cost_price, b.expiration_date, s.name " +
                "FROM batches b " +
                "LEFT JOIN suppliers s ON b.supplier_id = s.supplier_id " +
                "WHERE b.item_id = ? AND b.quantity_remaining > 0 " +
                "ORDER BY b.expiration_date ASC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, itemId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("batch_id"));
                row.add(rs.getString("batch_number"));
                row.add(rs.getInt("quantity_remaining"));
                row.add(rs.getDouble("cost_price"));
                row.add(rs.getDate("expiration_date"));
                row.add(rs.getString("name"));
                data.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new DefaultTableModel(data, columns);
    }
}