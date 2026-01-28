package com.pharmacy.inventory.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class PurchaseDAO {

    private final DataSource dataSource;
    private final AuditDAO auditDAO;

    @Autowired
    public PurchaseDAO(DataSource dataSource, AuditDAO auditDAO) {
        this.dataSource = dataSource;
        this.auditDAO = auditDAO;
    }


    public boolean createBulkPurchaseOrder(int supplierId, List<Object[]> items, long userId, String role) {
        long poId = System.currentTimeMillis();
        double totalAmount = items.stream().mapToDouble(i -> (int)i[1] * (double)i[2]).sum();
        int initialApproval = (role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("manager")) ? 1 : 0;

        String poSql = "INSERT INTO purchases (purchase_id, order_date, status, total_amount, supplier_id, is_approved, user_id, actual_amount) VALUES (?, CURDATE(), 'pending', ?, ?, ?, ?, 0.0)";
        String itemSql = "INSERT INTO purchase_items (purchase_id, item_id, quantity, unit_price, batch_number, is_received) VALUES (?, ?, ?, ?, 'PENDING', 0)";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psPo = conn.prepareStatement(poSql);
                 PreparedStatement psItem = conn.prepareStatement(itemSql)) {

                psPo.setLong(1, poId);
                psPo.setDouble(2, totalAmount);
                psPo.setInt(3, supplierId);
                psPo.setInt(4, initialApproval);
                psPo.setLong(5, userId);
                psPo.executeUpdate();

                for (Object[] item : items) {
                    psItem.setLong(1, poId);
                    psItem.setInt(2, (int)item[0]);
                    psItem.setInt(3, (int)item[1]);
                    psItem.setDouble(4, (double)item[2]);
                    psItem.addBatch();
                }
                psItem.executeBatch();

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

    public List<Long> getMyApprovedOrders(long userId) {
        List<Long> ids = new ArrayList<>();
        String sql = "SELECT purchase_id FROM purchases WHERE status = 'pending' AND is_approved = 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getLong(1));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return ids;
    }

    public boolean fulfillItemAndCheckClosure(long purchaseId, int itemId, double lineTotal) {
        String updateItem = "UPDATE purchase_items SET is_received = 1 WHERE purchase_id = ? AND item_id = ?";
        String updateActual = "UPDATE purchases SET actual_amount = actual_amount + ? WHERE purchase_id = ?";
        String checkRemaining = "SELECT COUNT(*) FROM purchase_items WHERE purchase_id = ? AND is_received = 0";
        String closePO = "UPDATE purchases SET status = 'delivered', received_at = NOW() WHERE purchase_id = ?";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psUpdate = conn.prepareStatement(updateItem);
                 PreparedStatement psActual = conn.prepareStatement(updateActual);
                 PreparedStatement psCheck = conn.prepareStatement(checkRemaining);
                 PreparedStatement psClose = conn.prepareStatement(closePO)) {

                psUpdate.setLong(1, purchaseId);
                psUpdate.setInt(2, itemId);
                psUpdate.executeUpdate();

                psActual.setDouble(1, lineTotal);
                psActual.setLong(2, purchaseId);
                psActual.executeUpdate();

                psCheck.setLong(1, purchaseId);
                ResultSet rs = psCheck.executeQuery();
                if (rs.next() && rs.getInt(1) == 0) {
                    psClose.setLong(1, purchaseId);
                    psClose.executeUpdate();
                    auditDAO.log("PO_FULLY_DELIVERED", "purchases", (int) purchaseId);
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public List<Object[]> getPendingItemsInPO(long purchaseId) {
        List<Object[]> items = new ArrayList<>();
        String sql = "SELECT pi.item_id, i.name, pi.quantity, pi.unit_price " +
                "FROM purchase_items pi JOIN items i ON pi.item_id = i.item_id " +
                "WHERE pi.purchase_id = ? AND pi.is_received = 0";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, purchaseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new Object[]{
                            rs.getInt("item_id"), rs.getString("name"),
                            rs.getInt("quantity"), rs.getDouble("unit_price")
                    });
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return items;
    }

    public List<Object[]> getPurchaseHistory() {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT p.purchase_id, s.name AS supplier_name, p.actual_amount, p.received_at " +
                "FROM purchases p JOIN suppliers s ON p.supplier_id = s.supplier_id " +
                "WHERE p.status = 'delivered' ORDER BY p.received_at DESC";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Object[]{
                        rs.getLong("purchase_id"), rs.getString("supplier_name"),
                        rs.getDouble("actual_amount"), rs.getTimestamp("received_at")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean rejectOrder(long id) {
        String sql = "DELETE FROM purchases WHERE purchase_id = ? AND is_approved = 0";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public List<Object[]> getActiveOrders() {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT p.purchase_id, s.name AS supplier_name, p.total_amount, u.name AS requester_name, p.is_approved " +
                "FROM purchases p JOIN suppliers s ON p.supplier_id = s.supplier_id " +
                "JOIN users u ON p.user_id = u.user_id WHERE p.status = 'pending' ORDER BY p.purchase_id DESC";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Object[]{
                        rs.getLong("purchase_id"), rs.getString("supplier_name"),
                        rs.getDouble("total_amount"), rs.getString("requester_name"),
                        rs.getInt("is_approved") == 1 ? "✅ Approved" : "⏳ Awaiting Admin"
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }


    public boolean approveOrder(long id) {
        String sql = "UPDATE purchases SET is_approved = 1 WHERE purchase_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                auditDAO.log("ORDER_APPROVED", "purchases", (int) id);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    public List<Object[]> getOrdersByStatus(String filterType) {
        List<Object[]> list = new ArrayList<>();
        String sql;

        if (filterType.equalsIgnoreCase("REQUESTED")) {
            // Only show orders that are NOT yet approved
            sql = "SELECT p.purchase_id, s.name AS supplier_name, p.total_amount, u.name AS requester_name, 'Awaiting Admin' " +
                    "FROM purchases p JOIN suppliers s ON p.supplier_id = s.supplier_id " +
                    "JOIN users u ON p.user_id = u.user_id WHERE p.status = 'pending' AND p.is_approved = 0";
        } else {
            return getActiveOrders(); // Fallback to your existing logic
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Object[]{
                        rs.getLong(1), rs.getString(2), rs.getDouble(3), rs.getString(4), rs.getString(5)
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public int getSupplierIdByPurchase(long id) {
        String sql = "SELECT supplier_id FROM purchases WHERE purchase_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("supplier_id");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public int getAwaitingApprovalCount() {
        String sql = "SELECT COUNT(*) FROM purchases WHERE status = 'pending' AND is_approved = 0";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int getAwaitingDeliveryCount() {
        String sql = "SELECT COUNT(*) FROM purchases WHERE status = 'pending' AND is_approved = 1";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
}