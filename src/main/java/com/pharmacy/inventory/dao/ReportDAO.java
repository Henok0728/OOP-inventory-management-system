package com.pharmacy.inventory.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ReportDAO {

    @Autowired
    private DataSource dataSource;

    public Map<String, Double> getFinancialSummary() {
        Map<String, Double> summary = new HashMap<>();


        String query =
                "SELECT " +
                        "  (SELECT COALESCE(SUM(total_amount), 0.0) FROM sales) as total_revenue, " +
                        "  (SELECT COALESCE(SUM(si.quantity * b.purchase_price), 0.0) " +
                        "   FROM sale_items si " +
                        "   LEFT JOIN batches b ON si.batch_id = b.batch_id) as cogs, " +
                        "  (SELECT COALESCE(SUM(w.quantity_removed * b.purchase_price), 0.0) " +
                        "   FROM waste w " +
                        "   LEFT JOIN batches b ON w.batch_id = b.batch_id) as waste_loss";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                double rev = rs.getDouble("total_revenue");
                double cogs = rs.getDouble("cogs");
                double waste = rs.getDouble("waste_loss");

                summary.put("REVENUE", rev);
                summary.put("COGS", cogs);
                summary.put("WASTE", waste);
                summary.put("PROFIT", rev - cogs - waste);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            summary.put("REVENUE", 0.0); summary.put("COGS", 0.0);
            summary.put("WASTE", 0.0); summary.put("PROFIT", 0.0);
        }
        return summary;
    }

    public List<Object[]> getRecentSalesSummary() {
        List<Object[]> list = new ArrayList<>();
        String query = "SELECT s.sale_date, COALESCE(c.first_name, 'Walk-in'), s.payment_method, s.total_amount " +
                "FROM sales s LEFT JOIN customers c ON s.customer_id = c.customer_id " +
                "ORDER BY s.sale_date DESC LIMIT 10";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new Object[]{rs.getTimestamp(1), rs.getString(2), rs.getString(3), rs.getDouble(4)});
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Object[]> getRecentPurchasesSummary() {
        List<Object[]> list = new ArrayList<>();
        String query = "SELECT b.received_date, i.name, b.batch_number, (b.quantity_received * b.purchase_price) " +
                "FROM batches b JOIN items i ON b.item_id = i.item_id " +
                "ORDER BY b.received_date DESC LIMIT 10";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new Object[]{rs.getDate(1), rs.getString(2), rs.getString(3), rs.getDouble(4)});
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
}