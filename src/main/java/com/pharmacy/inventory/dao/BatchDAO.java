package com.pharmacy.inventory.dao;

import com.pharmacy.inventory.model.Batch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.util.Vector;

@Repository
public class BatchDAO {

    @Autowired
    private DataSource dataSource;

    public boolean addBatch(Batch batch) {

        if (batch.getSellingPrice() <= batch.getPurchasePrice()) {
            System.err.println("Validation Error: Selling price must be higher than purchase price.");
            return false;
        }


        String sql = "INSERT INTO batches (batch_number, item_id, quantity_received, quantity_remaining, " +
                "manufactured_date, expiration_date, purchase_price, selling_price, " +
                "storage_location, status, received_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURDATE())";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {


            String bNum = (batch.getBatchNumber() == null || batch.getBatchNumber().isEmpty())
                    ? "BCH-" + System.currentTimeMillis() : batch.getBatchNumber();

            pstmt.setString(1, bNum);
            pstmt.setInt(2, batch.getItemId());
            pstmt.setInt(3, batch.getQuantityReceived());
            pstmt.setInt(4, batch.getQuantityReceived());
            pstmt.setString(5, batch.getManufacturedDate());
            pstmt.setString(6, batch.getExpirationDate());
            pstmt.setDouble(7, batch.getPurchasePrice());
            pstmt.setDouble(8, batch.getSellingPrice());
            pstmt.setString(9, batch.getStorageLocation());
            pstmt.setString(10, batch.getStatus() == null ? "active" : batch.getStatus());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding batch: " + e.getMessage());
            return false;
        }
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
        columns.add("Purchase Price");
        columns.add("Expiry Date");
        columns.add("Location");

        Vector<Vector<Object>> data = new Vector<>();

        String sql = "SELECT batch_id, batch_number, quantity_remaining, purchase_price, " +
                "expiration_date, storage_location " +
                "FROM batches " +
                "WHERE item_id = ? AND quantity_remaining > 0 " +
                "ORDER BY expiration_date ASC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, itemId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("batch_id"));
                row.add(rs.getString("batch_number"));
                row.add(rs.getInt("quantity_remaining"));
                row.add(rs.getDouble("purchase_price"));
                row.add(rs.getDate("expiration_date"));
                row.add(rs.getString("storage_location"));
                data.add(row);
            }
        } catch (SQLException e) {
            System.err.println("SQL Error in getBatchesByItem: " + e.getMessage());
        }
        return new DefaultTableModel(data, columns);
    }
}