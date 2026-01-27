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

    public boolean createPurchaseOrder(int supplierId, double estimatedTotal, long userId, String role) {
        String sql = "INSERT INTO purchases (purchase_id, order_date, status, total_amount, supplier_id, is_approved, user_id) " +
                "VALUES (?, CURDATE(), 'pending', ?, ?, ?, ?)";
        long id = System.currentTimeMillis();
        int initialApproval = (role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("manager")) ? 1 : 0;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setDouble(2, estimatedTotal);
            ps.setInt(3, supplierId);
            ps.setInt(4, initialApproval);
            ps.setLong(5, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean reconcileAndClose(long purchaseId, double actualTotal) {
        // Sets status to 'delivered' so it leaves all 'pending' counters
        String sql = "UPDATE purchases SET status = 'delivered', actual_amount = ?, received_at = NOW() " +
                "WHERE purchase_id = ? AND is_approved = 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, actualTotal);
            ps.setLong(2, purchaseId);
            if (ps.executeUpdate() > 0) {
                auditDAO.log("PO_RECONCILED_GRN", "purchases", (int) purchaseId);
                return true;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public double getApprovedAmount(long id) {
        String sql = "SELECT total_amount FROM purchases WHERE purchase_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("total_amount");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
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

    // KPI: POs created by staff but not yet signed by Admin
    public int getAwaitingApprovalCount() {
        String sql = "SELECT COUNT(*) FROM purchases WHERE status = 'pending' AND is_approved = 0";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // KPI: Approved POs waiting for physical stock entry (GRN)
    public int getAwaitingDeliveryCount() {
        String sql = "SELECT COUNT(*) FROM purchases WHERE status = 'pending' AND is_approved = 1";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
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

    public void approveOrder(long id) {
        String sql = "UPDATE purchases SET is_approved = 1 WHERE purchase_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
            auditDAO.log("ORDER_APPROVED", "purchases", (int) id);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public boolean rejectOrder(long id) {
        String sql = "DELETE FROM purchases WHERE purchase_id = ? AND is_approved = 0";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public List<Long> getMyApprovedOrders(long userId) {
        List<Long> ids = new ArrayList<>();
        // All approved pending orders should be fulfillable
        String sql = "SELECT purchase_id FROM purchases WHERE status = 'pending' AND is_approved = 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getLong(1));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return ids;
    }
}