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


    public boolean executeSale(Long customerId, DefaultTableModel cart, String paymentMethod, double totalAmount) {
        String insertSale = "INSERT INTO sales (customer_id, total_amount, payment_method, sale_date) VALUES (?, ?, ?, NOW())";
        String findBatches = "SELECT batch_id, quantity_remaining FROM batches " +
                "WHERE item_id = ? AND quantity_remaining > 0 AND status = 'active' " +
                "AND expiration_date > CURDATE() ORDER BY expiration_date ASC"; // FEFO Logic
        String updateBatch = "UPDATE batches SET quantity_remaining = quantity_remaining - ? WHERE batch_id = ?";
        String insertSaleItem = "INSERT INTO sale_items (sale_id, item_id, batch_id, quantity, unit_price, subtotal) VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            // 1. Create Sale Header
            long saleId = -1;
            try (PreparedStatement psSale = conn.prepareStatement(insertSale, Statement.RETURN_GENERATED_KEYS)) {
                if (customerId == null || customerId <= 0) psSale.setNull(1, Types.BIGINT);
                else psSale.setLong(1, customerId);
                psSale.setDouble(2, totalAmount);
                psSale.setString(3, paymentMethod);
                psSale.executeUpdate();
                ResultSet rs = psSale.getGeneratedKeys();
                if (rs.next()) saleId = rs.getLong(1);
            }

            // 2. Loop through Cart items
            for (int i = 0; i < cart.getRowCount(); i++) {
                int itemId = (int) cart.getValueAt(i, 0);
                int qtyNeeded = Integer.parseInt(cart.getValueAt(i, 2).toString());
                double unitPrice = (double) cart.getValueAt(i, 3);

                // 3. Find available batches for this item (FEFO)
                try (PreparedStatement psFind = conn.prepareStatement(findBatches)) {
                    psFind.setInt(1, itemId);
                    ResultSet rsBatch = psFind.executeQuery();

                    while (qtyNeeded > 0 && rsBatch.next()) {
                        int batchId = rsBatch.getInt("batch_id");
                        int available = rsBatch.getInt("quantity_remaining");
                        int take = Math.min(qtyNeeded, available);

                        // Update Batch
                        try (PreparedStatement psUpd = conn.prepareStatement(updateBatch)) {
                            psUpd.setInt(1, take);
                            psUpd.setInt(2, batchId);
                            psUpd.executeUpdate();
                        }

                        // Log Sale Item linked to this specific batch
                        try (PreparedStatement psItem = conn.prepareStatement(insertSaleItem)) {
                            psItem.setLong(1, saleId);
                            psItem.setInt(2, itemId);
                            psItem.setInt(3, batchId);
                            psItem.setInt(4, take);
                            psItem.setDouble(5, unitPrice);
                            psItem.setDouble(6, take * unitPrice);
                            psItem.executeUpdate();
                        }

                        qtyNeeded -= take;
                    }

                    if (qtyNeeded > 0) throw new SQLException("Insufficient unexpired stock for item ID: " + itemId);
                }
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }
    // --- DASHBOARD METHODS (Used by HomePanel) ---

    public double getTodaysProfit() {
        // We filter by s.total_amount > 0 to exclude voided/cancelled transactions
        String sql = "SELECT SUM((si.unit_price - b.purchase_price) * si.quantity) " +
                "FROM sale_items si " +
                "JOIN sales s ON si.sale_id = s.sale_id " +
                "JOIN batches b ON si.batch_id = b.batch_id " +
                "WHERE DATE(s.sale_date) = CURDATE() " +
                "AND s.total_amount > 0";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {


            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            System.err.println("Profit calculation error: " + e.getMessage());
        }
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

    public DefaultTableModel getSalesHistory() {
        String[] columns = {"Sale ID", "Date", "Customer", "Total (ETB)", "Payment Status", "Discount"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);

        // We use the alias 'payment_status' to handle the VOIDED label
        String sql = "SELECT s.sale_id, s.sale_date, " +
                "COALESCE(CONCAT(c.first_name, ' ', c.last_name), 'Walk-in') as customer_name, " +
                "s.total_amount, " +
                "CASE WHEN s.total_amount = 0 THEN 'VOIDED' ELSE s.payment_method END as payment_status, " +
                "s.discount " +
                "FROM sales s " +
                "LEFT JOIN customers c ON s.customer_id = c.customer_id " +
                "ORDER BY s.sale_date DESC";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("sale_id"),
                        rs.getTimestamp("sale_date"),
                        rs.getString("customer_name"),
                        rs.getDouble("total_amount"),
                        rs.getString("payment_status"),
                        rs.getDouble("discount")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return model;
    }

    public DefaultTableModel getSaleItems(int saleId) {
        String[] columns = {"Item Name", "Quantity", "Unit Price", "Subtotal"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);

        String sql = "SELECT i.name, si.quantity, si.unit_price, si.subtotal " +
                "FROM sale_items si " +
                "JOIN items i ON si.item_id = i.item_id " +
                "WHERE si.sale_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("name"),
                        rs.getInt("quantity"),
                        rs.getDouble("unit_price"),
                        rs.getDouble("subtotal")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return model;
    }

    public boolean voidSale(int saleId) {
        String getItemsSql = "SELECT item_id, batch_id, quantity FROM sale_items WHERE sale_id = ?";
        String updateBatchSql = "UPDATE batches SET quantity_remaining = quantity_remaining + ? WHERE batch_id = ?";
        String updateSaleSql = "UPDATE sales SET total_amount = 0, discount = 0 WHERE sale_id = ?";

        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            // 1. Restock Batches
            try (PreparedStatement psGet = conn.prepareStatement(getItemsSql)) {
                psGet.setInt(1, saleId);
                ResultSet rs = psGet.executeQuery();

                try (PreparedStatement psBatch = conn.prepareStatement(updateBatchSql)) {
                    while (rs.next()) {
                        psBatch.setInt(1, rs.getInt("quantity"));
                        psBatch.setInt(2, rs.getInt("batch_id"));
                        psBatch.addBatch();
                    }
                    psBatch.executeBatch();
                }
            }

            // 2. Zero out the sale amount
            try (PreparedStatement psSale = conn.prepareStatement(updateSaleSql)) {
                psSale.setInt(1, saleId);
                psSale.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
        }
    }
}