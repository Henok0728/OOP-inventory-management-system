package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class HomePanel extends JPanel {
    private final SalesDAO salesDAO;
    private final ItemDAO itemDAO;
    private final BatchDAO batchDAO;

    public HomePanel(SalesDAO salesDAO, ItemDAO itemDAO, BatchDAO batchDAO) {
        this.salesDAO = salesDAO;
        this.itemDAO = itemDAO;
        this.batchDAO = batchDAO;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(245, 245, 245));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        initializeUI();
    }

    private void initializeUI() {
        // Clear everything first (important for refresh)
        this.removeAll();

        // --- SECTION 1: TOP KPI CARDS ---
        JPanel kpiContainer = new JPanel(new GridLayout(1, 4, 15, 0));
        kpiContainer.setOpaque(false);
        kpiContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        // Real Profit (ETB)
        double profit = salesDAO.getTodaysProfit();
        salesDAO.getTodaysProfit();
        kpiContainer.add(createKpiCard("Today's Profit", String.format("%.2f ETB", profit), new Color(40, 167, 69)));

        // Real Revenue (ETB)
        double revenue = salesDAO.getTodaysSalesRevenue();
        kpiContainer.add(createKpiCard("Today's Revenue", String.format("%.2f ETB", revenue), new Color(0, 123, 255)));

        // Real Expiry Count
        int expiringCount = batchDAO.getExpiringBatchesModel().getRowCount();
        kpiContainer.add(createKpiCard("Expiring Soon", expiringCount + " Batches", new Color(220, 53, 69)));

        // Real Low Stock Count
        int lowStockCount = batchDAO.getLowStockBatchesModel().getRowCount();
        kpiContainer.add(createKpiCard("Low Stock", lowStockCount + " Items", new Color(255, 193, 7)));

        add(kpiContainer);
        add(Box.createRigidArea(new Dimension(0, 20)));

        // --- SECTION 2: EXPIRATION ALERTS ---
        add(createSectionPanel("⚠️ Expiration Alerts (Within 30 days)",
                batchDAO.getExpiringBatchesModel()));

        add(Box.createRigidArea(new Dimension(0, 20)));

        // --- SECTION 3: REORDER ALERTS ---
        add(createSectionPanel("📦 Reorder Alerts (Low Stock)",
                batchDAO.getLowStockBatchesModel()));

        add(Box.createRigidArea(new Dimension(0, 20)));

        // --- SECTION 4: TODAY'S SALES ACTIVITY ---
        add(createSectionPanel("🛒 Today's Sales Activity",
                salesDAO.getTodaysSalesDetailsModel()));

        add(Box.createVerticalGlue());

        // Refresh UI components
        this.revalidate();
        this.repaint();
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
        container.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
        container.setPreferredSize(new Dimension(0, 250));

        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)), title);
        border.setTitleFont(new Font("SansSerif", Font.BOLD, 14));
        container.setBorder(border);

        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.getTableHeader().setReorderingAllowed(false);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean isS, boolean hasF, int r, int c) {
                String text = (v == null) ? "" : v.toString().replace("$", "ETB ");
                return super.getTableCellRendererComponent(t, text, isS, hasF, r, c);
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        container.add(scroll, BorderLayout.CENTER);
        return container;
    }

    public void refreshData() {
        initializeUI();
    }
}