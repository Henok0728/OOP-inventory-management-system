package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class HomePanel extends JPanel {

    public HomePanel(SalesDAO salesDAO, ItemDAO itemDAO, BatchDAO batchDAO) {
        // Use a vertical BoxLayout to allow "infinite" scrolling down
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(245, 245, 245));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- SECTION 1: TOP KPI CARDS ---
        JPanel kpiContainer = new JPanel(new GridLayout(1, 4, 15, 0));
        kpiContainer.setOpaque(false);
        kpiContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        kpiContainer.add(createKpiCard("Today's Profit", "$" + salesDAO.getTodaysProfit(), new Color(40, 167, 69)));
        kpiContainer.add(createKpiCard("Today's Sales", salesDAO.getTodaysSalesCount() + " Items", new Color(0, 123, 255)));
        kpiContainer.add(createKpiCard("Expiring Soon", "Check Alerts", new Color(220, 53, 69)));
        kpiContainer.add(createKpiCard("Low Stock", "Check Batches", new Color(255, 193, 7)));

        add(kpiContainer);
        add(Box.createRigidArea(new Dimension(0, 20))); // Spacing

        // --- SECTION 2: EXPIRATION ALERTS ---
        // logic: Fetch items where expiry date is within 30 days
        add(createSectionPanel("⚠️ Expiration Alerts (Expiring within 30 days)",
                batchDAO.getExpiringBatchesModel()));

        add(Box.createRigidArea(new Dimension(0, 20)));

        // --- SECTION 3: LOW STOCK / FINISHING BATCHES ---
        // logic: Fetch batches where quantity < 10
        add(createSectionPanel("📦 Reorder Alerts (Low Stock / Finishing Batches)",
                batchDAO.getLowStockBatchesModel()));

        add(Box.createRigidArea(new Dimension(0, 20)));

        // --- SECTION 4: TODAY'S SOLD PRODUCTS ---
        add(createSectionPanel("🛒 Today's Sales Activity",
                salesDAO.getTodaysSalesDetailsModel()));

        add(Box.createVerticalGlue()); // Pushes everything to the top
    }

    private JPanel createKpiCard(String title, String value, Color accent) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel tL = new JLabel(title);
        tL.setForeground(Color.GRAY);
        JLabel vL = new JLabel(value);
        vL.setFont(new Font("SansSerif", Font.BOLD, 22));

        JPanel bar = new JPanel();
        bar.setPreferredSize(new Dimension(5, 0));
        bar.setBackground(accent);

        card.add(bar, BorderLayout.WEST);
        card.add(tL, BorderLayout.NORTH);
        card.add(vL, BorderLayout.CENTER);
        return card;
    }

    private JPanel createSectionPanel(String title, DefaultTableModel model) {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        container.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250)); // Fixed height for each table
        container.setPreferredSize(new Dimension(0, 250));

        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)), title);
        border.setTitleFont(new Font("SansSerif", Font.BOLD, 14));
        container.setBorder(border);

        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.getTableHeader().setReorderingAllowed(false);

        // Custom Renderer for Null Safety
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean isS, boolean hasF, int r, int c) {
                String text = (v == null) ? "" : v.toString();
                return super.getTableCellRendererComponent(t, text, isS, hasF, r, c);
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        container.add(scroll, BorderLayout.CENTER);
        return container;
    }
}