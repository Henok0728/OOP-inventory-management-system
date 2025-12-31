package com.pharmacy.inventory.dao;

import com.pharmacy.inventory.model.Batch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import javax.swing.table.DefaultTableModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class BatchDAO {

    @Autowired
    private DataSource dataSource;

    public void addBatch(Batch batch) {
        String sql = "INSERT INTO batches (batch_number, item_id, quantity_received, quantity_remaining, " +
                "expiration_date, purchase_price, selling_price, status, received_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, batch.getBatchNumber());
            pstmt.setInt(2, batch.getItemId());
            pstmt.setInt(3, batch.getQuantityReceived());
            pstmt.setInt(4, batch.getQuantityRemaining());
            pstmt.setString(5, batch.getExpirationDate());
            pstmt.setDouble(6, batch.getPurchasePrice());
            pstmt.setDouble(7, batch.getSellingPrice());
            pstmt.setString(8, batch.getStatus());
            pstmt.setString(9, batch.getReceivedDate());

            pstmt.executeUpdate();
            System.out.println("Batch added successfully!");

        } catch (SQLException e) {
            System.err.println("Error adding batch: " + e.getMessage());
        }
    }

    public List<Batch> getBatchesByItem(int itemId) {
        List<Batch> batches = new ArrayList<>();
        String sql = "SELECT * FROM batches WHERE item_id = ? AND quantity_remaining > 0 " +
                "AND status = 'active' ORDER BY expiration_date ASC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Batch batch = new Batch();
                batch.setBatchId(rs.getLong("batch_id"));
                batch.setBatchNumber(rs.getString("batch_number"));
                batch.setItemId(rs.getInt("item_id"));
                batch.setQuantityRemaining(rs.getInt("quantity_remaining"));
                batch.setExpirationDate(rs.getString("expiration_date"));
                batch.setSellingPrice(rs.getDouble("selling_price"));
                batch.setStatus(rs.getString("status"));

                batches.add(batch);
            }
        } catch (SQLException e) {
            System.err.println("Error finding batches: " + e.getMessage());
        }
        return batches;
    }

    public void deleteBatch(int batch_id){
        String sql = "DELETE from batchs WHERE batch_id = ?";

        try(Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setInt(1, batch_id);

            pstmt.executeUpdate();
        }
        catch(SQLException e){
            System.out.println("Error deleting batch!");
        }
    }

    public DefaultTableModel getBatchesByItemIdModel(int itemId) {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"ID", "Batch #", "Qty Remaining", "Expiry Date", "Price", "Status"}, 0);

        String sql = "SELECT batch_id, batch_number, quantity_remaining, expiration_date, selling_price, status " +
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
                        "$" + String.format("%.2f", rs.getDouble("selling_price")),
                        rs.getString("status")
                });
            }
        } catch (SQLException e) {
            System.err.println("Error loading batch model: " + e.getMessage());
        }
        return model;
    }

    public DefaultTableModel getLowStockBatchesModel() {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Item Name", "Batch #", "Qty Left"}, 0);

        // SQL joins the Item table to get the Name, filters by quantity
        String sql = "SELECT i.name, b.batch_number, b.quantity_remaining " +
                "FROM batches b JOIN items i ON b.item_id = i.item_id " +
                "WHERE b.quantity_remaining < 10 AND b.status = 'active'";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

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
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Item Name", "Batch #", "Expiry Date"}, 0);

        // SQL finds batches expiring in the next 30 days
        String sql = "SELECT i.name, b.batch_number, b.expiration_date " +
                "FROM batches b JOIN items i ON b.item_id = i.item_id " +
                "WHERE b.expiration_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY) " +
                "AND b.expiration_date >= CURDATE()";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

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

}