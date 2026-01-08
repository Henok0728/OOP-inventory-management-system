package com.pharmacy.inventory.dao;

import javax.sql.DataSource;

import com.pharmacy.inventory.model.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.swing.table.DefaultTableModel;
import java.sql.*;

@Repository
public class ItemDAO {


    @Autowired
    private DataSource dataSource;

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
            System.out.println("Something went wrong while, fetching all items!");
        }
        return dtm;
    }

    public int insertItem(Item item) {
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

            // Now this will work because we requested the keys above
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error inserting item: " + e.getMessage());
            return -1;
        }
        return -1;
    }

    public DefaultTableModel searchItems(String query) {
        String sql = "SELECT i.item_id, i.name, i.brand_name, i.category, i.retail_price, " +
                "b.quantity_remaining, b.expiration_date, i.prescription_required " +
                "FROM items i " +
                "LEFT JOIN batches b ON i.item_id = b.item_id " +
                "WHERE i.name LIKE ? OR i.brand_name LIKE ? OR i.barcode LIKE ?";

        String[] columnNames = {"Item N0","Product Name", "Brand", "Category", "Price", "Stock", "Expiry", "Rx"};
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
                        rs.getInt("quantity_remaining"),
                        rs.getString("expiration_date"),
                        rs.getBoolean("prescription_required") ? "YES" : "NO"
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return model;
    }

    public void insertItemWithBatch(Item item, String bNum, int qty, double pPrice, String expiry) {
        String itemSql = "INSERT INTO items (name, generic_name, brand_name, barcode, category, " +
                "dosage_form, strength, retail_price, reorder_level, prescription_required) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String batchSql = "INSERT INTO batches (batch_number, item_id, quantity_received, quantity_remaining, " +
                "expiration_date, purchase_price, selling_price, status, received_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 'active', CURDATE())";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false); // Start transaction

            try (PreparedStatement itemStmt = conn.prepareStatement(itemSql, Statement.RETURN_GENERATED_KEYS)) {
                // --- YOU MUST SET THESE VALUES BEFORE itemStmt.executeUpdate() ---
                itemStmt.setString(1, item.getName());
                itemStmt.setString(2, item.getGenericName());
                itemStmt.setString(3, item.getBrandName());
                itemStmt.setString(4, item.getBarcode());
                itemStmt.setString(5, item.getCategory());
                itemStmt.setString(6, item.getDosageForm());
                itemStmt.setString(7, item.getStrength());
                itemStmt.setDouble(8, item.getRetailPrice());
                itemStmt.setInt(9, item.getReorderLevel());
                itemStmt.setInt(10, item.isPrescriptionRequired() ? 1 : 0);
                itemStmt.executeUpdate();

                ResultSet rs = itemStmt.getGeneratedKeys();
                if (rs.next()) {
                    int newItemId = rs.getInt(1);

                    try (PreparedStatement batchStmt = conn.prepareStatement(batchSql)) {
                        batchStmt.setString(1, bNum);
                        batchStmt.setInt(2, newItemId);
                        batchStmt.setInt(3, qty);      // quantity_received
                        batchStmt.setInt(4, qty);      // quantity_remaining
                        batchStmt.setString(5, expiry); // expiration_date
                        batchStmt.setDouble(6, pPrice); // purchase_price
                        batchStmt.setDouble(7, item.getRetailPrice()); // selling_price

                        batchStmt.executeUpdate();
                    }
                }
                conn.commit();
                System.out.println("Success: Item and Batch created.");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Transaction Error: " + e.getMessage());
        }
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
            pstmt.setInt(9, item.getReorderLevel()); // Dynamic update
            pstmt.setInt(10, item.isPrescriptionRequired() ? 1 : 0);
            pstmt.setInt(11, item.getItem_id());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating item: " + e.getMessage());
        }
    }

    public void removeItem(int item_id){
        String sql = "DELETE FROM items WHERE item_id = ?";

        try(Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setInt(1,item_id);

            int row = pstmt.executeUpdate();

        }
        catch(SQLException e) {
            System.out.println("Error deleting an item!");
        }
    }

    public int getAnyAvailableBatch(int itemId) {
        // This provides the batch_id required by your sale_items schema
        String sql = "SELECT batch_id FROM batches WHERE item_id = ? AND quantity_remaining > 0 LIMIT 1";
        try (java.sql.Connection conn = dataSource.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("batch_id");
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return -1; // Indicates no stock available
    }

    // Method 1: Finds the item details based on the barcode scanned in SalesPanel
    // Here is where the hardware implementation goes
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
                    // Add other setters if your Item model requires them
                    return item;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }




}
