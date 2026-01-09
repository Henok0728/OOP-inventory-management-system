package com.pharmacy.inventory.dao;

import javax.sql.DataSource;
import com.pharmacy.inventory.model.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ItemDAO {

    @Autowired
    private DataSource dataSource;

    /**
     * Fetches all items for the ProductsPanel table.
     * Includes a calculated total stock from all active batches.
     */
    public DefaultTableModel getAllItems() {
        DefaultTableModel dtm = new DefaultTableModel();
        String sql = "SELECT i.item_id, i.name, i.generic_name, i.brand_name, i.barcode, " +
                "i.category, i.dosage_form, i.strength, i.retail_price, i.reorder_level, " +
                "i.prescription_required, COALESCE(SUM(b.quantity_remaining), 0) AS total_stock " +
                "FROM items i LEFT JOIN batches b ON i.item_id = b.item_id " +
                "GROUP BY i.item_id, i.name, i.generic_name, i.brand_name, i.barcode, " +
                "i.category, i.dosage_form, i.strength, i.retail_price, i.reorder_level, " +
                "i.prescription_required";

        try (Connection con = dataSource.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData rm = rs.getMetaData();
            for (int i = 1; i <= rm.getColumnCount(); i++) {
                dtm.addColumn(rm.getColumnName(i));
            }

            while (rs.next()) {
                Object[] row = new Object[rm.getColumnCount()];
                for (int i = 1; i <= rm.getColumnCount(); i++) {
                    row[i - 1] = rs.getObject(i);
                }
                dtm.addRow(row);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching items: " + e.getMessage());
        }
        return dtm;
    }
    public int addItem(Item item) {
        String sql = "INSERT INTO items (name, generic_name, brand_name, barcode, category, " +
                "dosage_form, strength, retail_price, reorder_level, prescription_required) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, item.getName());
            pstmt.setString(2, item.getGenericName());
            pstmt.setString(3, item.getBrandName());
            pstmt.setString(4, item.getBarcode());
            pstmt.setString(5, item.getCategory().toLowerCase());
            pstmt.setString(6, item.getDosageForm());
            pstmt.setString(7, item.getStrength());
            pstmt.setDouble(8, item.getRetailPrice());
            pstmt.setInt(9, item.getReorderLevel());
            pstmt.setInt(10, item.isPrescriptionRequired() ? 1 : 0);

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error inserting item: " + e.getMessage());
        }
        return -1;
    }

    public DefaultTableModel searchItems(String query) {
        // We join with batches so the salesperson knows if there is enough stock
        String sql = "SELECT i.item_id, i.name, i.brand_name, i.category, i.retail_price, " +
                "SUM(b.quantity_remaining) as total_stock, MAX(b.expiration_date) as latest_expiry, " +
                "i.prescription_required " +
                "FROM items i " +
                "LEFT JOIN batches b ON i.item_id = b.item_id " +
                "WHERE i.name LIKE ? OR i.brand_name LIKE ? OR i.barcode LIKE ? " +
                "GROUP BY i.item_id";

        String[] columnNames = {"ID", "Product Name", "Brand", "Category", "Price (ETB)", "Stock", "Expiry", "Rx"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);

        String searchPattern = "%" + query + "%";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("item_id"),
                        rs.getString("name"),
                        rs.getString("brand_name"),
                        rs.getString("category"),
                        rs.getBigDecimal("retail_price"),
                        rs.getInt("total_stock"),
                        rs.getString("latest_expiry"),
                        rs.getBoolean("prescription_required") ? "YES" : "NO"
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return model;
    }

    public void updateItem(Item item) {
        String sql = "UPDATE items SET name=?, generic_name=?, brand_name=?, barcode=?, " +
                "category=?, dosage_form=?, strength=?, retail_price=?, " +
                "reorder_level=?, prescription_required=? WHERE item_id=?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item.getName());
            pstmt.setString(2, item.getGenericName());
            pstmt.setString(3, item.getBrandName());
            pstmt.setString(4, item.getBarcode());
            pstmt.setString(5, item.getCategory());
            pstmt.setString(6, item.getDosageForm());
            pstmt.setString(7, item.getStrength());
            pstmt.setDouble(8, item.getRetailPrice());
            pstmt.setInt(9, item.getReorderLevel());
            pstmt.setInt(10, item.isPrescriptionRequired() ? 1 : 0);
            pstmt.setInt(11, item.getItem_id());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating item: " + e.getMessage());
        }
    }

    public void removeItem(int item_id) {
        String sql = "DELETE FROM items WHERE item_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, item_id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting item: " + e.getMessage());
        }
    }

    // --- HELPER METHODS FOR SALES/HARDWARE ---

    public Item findItemByBarcode(String barcode) {
        String sql = "SELECT * FROM items WHERE barcode = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, barcode);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Item item = new Item();
                    item.setItem_id(rs.getInt("item_id"));
                    item.setName(rs.getString("name"));
                    item.setRetailPrice(rs.getDouble("retail_price"));
                    return item;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public int getAnyAvailableBatch(int itemId) {
        String sql = "SELECT batch_id FROM batches WHERE item_id = ? AND quantity_remaining > 0 LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("batch_id");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public List<Item> getAllItemsList() {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT item_id, name, strength, dosage_form FROM items ORDER BY name ASC";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Item item = new Item();
                item.setItem_id(rs.getInt("item_id"));
                item.setName(rs.getString("name"));
                item.setStrength(rs.getString("strength"));
                item.setDosageForm(rs.getString("dosage_form"));
                items.add(item);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching item list: " + e.getMessage());
        }
        return items;
    }


}