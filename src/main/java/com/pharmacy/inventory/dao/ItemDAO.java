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
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public DefaultTableModel getAllItems() {
        DefaultTableModel dtm = new DefaultTableModel();
        String sql = "SELECT * FROM items";

        try(Connection con = dataSource.getConnection();
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData rm = rs.getMetaData();

            for(int i = 1; i <= rm.getColumnCount(); i++){
                dtm.addColumn(rm.getColumnName(i));
            }

            while(rs.next()){
                Object[] row = new Object[rm.getColumnCount()];
                for(int i = 1; i <= rm.getColumnCount(); i++){
                    row[i-1] = rs.getObject(i);
                }
                dtm.addRow(row);
            }
        }
        catch (SQLException e) {
            System.out.println("Something went wrong!");
        }
        return dtm;
    }

    public void insertItem(Item item){
        String sql = "INSERT INTO items(name, generic_name, brand_name, barcode, category" +
                     "dosage_form, strength, retail_price, prescription_required) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

//        try(Connection conn = dataSource.getConnection();
//            PreparedStatement pstmt = conn.prepareStatement(sql)){
//            pstmt.setString(1, item.getName());
//            pstmt.setString(2, item.getGenericName());
//            pstmt.setString(3, item.getBrandName());
//            pstmt.setString(4, item.getBarcode());
//            pstmt.setString(5, item.getCategory().toLowerCase())
//            pstmt.setString(6, item.getDosageForm());
//            pstmt.setString(7, item.getStrength());
//            pstmt.setDouble(8, item.getRetailPrice());
//            pstmt.setInt(9, item.isPrescriptionRequired() ? 1 : 0);
//
//            int row = pstmt.executeUpdate();
//        }
//        catch(SQLException e) {
//            System.out.println("Error inserting item: " + e.getMessage());
//        }

        //Using the jdbc template
        jdbcTemplate.update(sql, item.getName(),item.getGenericName(),
                item.getBrandName(), item.getBarcode(),
                item.getCategory(), item.getDosageForm(),
                item.getStrength(), item.getRetailPrice(),
                item.isPrescriptionRequired() ? 1 : 0);
    }


}
