package com.pharmacy.inventory.dao;

import com.pharmacy.inventory.model.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SupplierDAO {

    @Autowired
    private DataSource dataSource;

    public List<Supplier> getAllSuppliers() {
        List<Supplier> list = new ArrayList<>();
        String sql = "SELECT * FROM suppliers ORDER BY name ASC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToSupplier(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean addSupplier(Supplier s) {
        String sql = "INSERT INTO suppliers (name, contact, phone_number, email, address, license_number, payment_terms) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, s.getName());
            ps.setString(2, s.getContact());
            ps.setString(3, s.getPhoneNumber());
            ps.setString(4, s.getEmail());
            ps.setString(5, s.getAddress());
            ps.setString(6, s.getLicenseNumber());
            ps.setString(7, s.getPaymentTerms());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Supplier> searchSuppliers(String query) {
        List<Supplier> list = new ArrayList<>();
        String sql = "SELECT * FROM suppliers WHERE name LIKE ? OR contact LIKE ? ORDER BY name ASC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String pattern = "%" + query + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSupplier(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Link an Item to a Supplier in the 'item_suppliers' table.
     */
    public void linkItemToSupplier(int itemId, int supplierId) {
        String sql = "INSERT INTO item_suppliers (item_id, supplied_id) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.setInt(2, supplierId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public DefaultTableModel getSupplierTableModel(List<Supplier> suppliers) {
        String[] columns = {"ID", "Supplier Name", "Contact Person", "Phone", "Email", "Address"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        for (Supplier s : suppliers) {
            model.addRow(new Object[]{
                    s.getSupplierId(),
                    s.getName(),
                    s.getContact(),
                    s.getPhoneNumber(),
                    s.getEmail(),
                    s.getAddress()
            });
        }
        return model;
    }

    // Utility method to convert DB rows to Java Objects
    private Supplier mapResultSetToSupplier(ResultSet rs) throws SQLException {
        return new Supplier(
                rs.getInt("supplier_id"),
                rs.getString("name"),
                rs.getString("license_number"),
                rs.getString("address"),
                rs.getString("email"),
                rs.getString("contact"),
                rs.getString("phone_number"),
                rs.getString("payment_terms")
        );
    }

    // Get total count for the first KPI card
    public int getTotalSuppliersCount() {
        String sql = "SELECT COUNT(*) FROM suppliers";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }


    public int getActiveSuppliersCount() {
        String sql = "SELECT COUNT(DISTINCT supplied_id) FROM item_suppliers";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }


    public DefaultTableModel getAllSuppliersModel() {
        return getSupplierTableModel(getAllSuppliers());
    }
}