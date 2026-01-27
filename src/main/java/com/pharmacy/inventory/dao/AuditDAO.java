package com.pharmacy.inventory.dao;

import com.pharmacy.inventory.util.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.Vector;

@Repository
public class AuditDAO {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public AuditDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void logAction(long userId, String action, String description) {
        String sql = "INSERT INTO audit_logs (user_id, action, table_affected) VALUES (?, ?, ?)";

        Object idToStore = (userId == 0) ? null : userId;

        jdbcTemplate.update(sql, idToStore, action, description);
    }

    public void log(String action, String table, Integer recordId) {
        String sql = "INSERT INTO audit_logs (user_id, action, table_affected, record_id) VALUES (?, ?, ?, ?)";
        long userId = UserSession.getCurrentUser() != null ? UserSession.getCurrentUser().getUserId() : 0;

        jdbcTemplate.update(sql, userId, action, table, recordId);
    }

    public DefaultTableModel getAllLogs() {
        Vector<String> columnNames = new Vector<>();
        columnNames.add("Log ID");
        columnNames.add("Staff");
        columnNames.add("Action");
        columnNames.add("Details/Table");
        columnNames.add("Record ID");
        columnNames.add("Timestamp");

        Vector<Vector<Object>> data = new Vector<>();
        String sql = "SELECT a.log_id, COALESCE(u.name, 'SYSTEM'), a.action, a.table_affected, a.record_id, a.timestamp " +
                "FROM audit_logs a LEFT JOIN users u ON a.user_id = u.user_id " +
                "ORDER BY a.timestamp DESC LIMIT 200";

        // Querying using JdbcTemplate
        List<Vector<Object>> rows = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Vector<Object> row = new Vector<>();
            row.add(rs.getInt(1));
            row.add(rs.getString(2));
            row.add(rs.getString(3));
            row.add(rs.getString(4));
            row.add(rs.getObject(5));
            row.add(rs.getTimestamp(6));
            return row;
        });

        data.addAll(rows);
        return new DefaultTableModel(data, columnNames);
    }
}