package com.pharmacy.inventory.dao;

import com.pharmacy.inventory.util.UserSession;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.util.Vector;

@Repository
public class AuditDAO {
    private final DataSource dataSource;

    public AuditDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // Method to save a log entry
    public void log(String action, String table, Integer recordId) {
        String sql = "INSERT INTO audit_logs (user_id, action, table_affected, record_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, UserSession.getCurrentUser().getUserId());
            ps.setString(2, action);
            ps.setString(3, table);
            if (recordId != null) ps.setInt(4, recordId);
            else ps.setNull(4, Types.INTEGER);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Method to fetch logs for the UI
    public DefaultTableModel getAllLogs() {
        Vector<String> columnNames = new Vector<>();
        columnNames.add("Log ID"); columnNames.add("Staff");
        columnNames.add("Action"); columnNames.add("Table");
        columnNames.add("Record ID"); columnNames.add("Timestamp");

        Vector<Vector<Object>> data = new Vector<>();
        String sql = "SELECT a.log_id, u.name, a.action, a.table_affected, a.record_id, a.timestamp " +
                "FROM audit_logs a JOIN users u ON a.user_id = u.user_id " +
                "ORDER BY a.timestamp DESC LIMIT 200";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt(1)); row.add(rs.getString(2));
                row.add(rs.getString(3)); row.add(rs.getString(4));
                row.add(rs.getObject(5)); row.add(rs.getTimestamp(6));
                data.add(row);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return new DefaultTableModel(data, columnNames);
    }
}