package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.*;
import com.pharmacy.inventory.service.EnvironmentService;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class HomePanel extends JPanel {
    private final SalesDAO salesDAO;
    private final ItemDAO itemDAO;
    private final BatchDAO batchDAO;
    private Timer refreshTimer;

    public HomePanel(SalesDAO salesDAO, ItemDAO itemDAO, BatchDAO batchDAO) {
        this.salesDAO = salesDAO;
        this.itemDAO = itemDAO;
        this.batchDAO = batchDAO;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(245, 245, 245));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        initializeUI();

        // Auto-refresh UI every 5 seconds to update sensor data
        refreshTimer = new Timer(5000, e -> refreshData());
        refreshTimer.start();
    }

    private void initializeUI() {
        this.removeAll();

        // --- SECTION 1: SENSOR & BUSINESS KPI CARDS ---
        // Expanded to 1 row, 6 columns to fit Temperature and Humidity
        JPanel kpiContainer = new JPanel(new GridLayout(1, 6, 10, 0));
        kpiContainer.setOpaque(false);
        kpiContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        // 1. Profit
        double profit = salesDAO.getTodaysProfit();
        kpiContainer.add(createKpiCard("Today's Profit", String.format("%.2f ETB", profit), new Color(40, 167, 69)));

        // 2. Revenue
        double revenue = salesDAO.getTodaysSalesRevenue();
        kpiContainer.add(createKpiCard("Today's Revenue", String.format("%.2f ETB", revenue), new Color(0, 123, 255)));

        // 3. Expiry
        int expiringCount = batchDAO.getExpiringBatchesModel().getRowCount();
        kpiContainer.add(createKpiCard("Expiring Soon", expiringCount + " Batches", new Color(220, 53, 69)));

        // 4. Low Stock
        int lowStockCount = batchDAO.getLowStockBatchesModel().getRowCount();
        kpiContainer.add(createKpiCard("Low Stock", lowStockCount + " Items", new Color(255, 193, 7)));

        // --- ARDUINO SENSOR DATA ---
        double temp = EnvironmentService.getTemp();
        double hum = EnvironmentService.getHum();

        // Color coding for temperature (Alert if > 25°C or < 15°C)
        Color tempColor = (temp > 25.0 || temp < 15.0 && temp != 0) ? new Color(220, 53, 69) : new Color(40, 167, 69);

        // 5. Temperature Card
        kpiContainer.add(createKpiCard("Warehouse Temp", String.format("%.1f °C", temp), tempColor));

        // 6. Humidity Card
        kpiContainer.add(createKpiCard("Humidity", String.format("%.1f %%", hum), new Color(23, 162, 184)));

        add(kpiContainer);
        add(Box.createRigidArea(new Dimension(0, 15)));

        // --- CRITICAL TEMPERATURE ALERT BAR ---
        if (temp > 25.0) {
            add(createAlertBar("⚠️ CRITICAL TEMPERATURE ALERT: WAREHOUSE EXCEEDS 25°C - MEDICINE QUALITY AT RISK!"));
            add(Box.createRigidArea(new Dimension(0, 15)));
        }

        // --- SECTION 2: ALERTS & ACTIVITY ---
        add(createSectionPanel("⚠️ Expiration Alerts (Within 30 days)", batchDAO.getExpiringBatchesModel()));
        add(Box.createRigidArea(new Dimension(0, 20)));

        add(createSectionPanel("📦 Reorder Alerts (Low Stock)", batchDAO.getLowStockBatchesModel()));
        add(Box.createRigidArea(new Dimension(0, 20)));

        add(createSectionPanel("🛒 Today's Sales Activity", salesDAO.getTodaysSalesDetailsModel()));

        add(Box.createVerticalGlue());

        this.revalidate();
        this.repaint();
    }

    private JPanel createKpiCard(String title, String value, Color accent) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        JLabel tL = new JLabel(title);
        tL.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tL.setForeground(Color.GRAY);

        JLabel vL = new JLabel(value);
        vL.setFont(new Font("SansSerif", Font.BOLD, 18));

        JPanel bar = new JPanel();
        bar.setPreferredSize(new Dimension(4, 0));
        bar.setBackground(accent);

        card.add(bar, BorderLayout.WEST);
        card.add(tL, BorderLayout.NORTH);
        card.add(vL, BorderLayout.CENTER);
        return card;
    }

    private JPanel createAlertBar(String message) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(255, 235, 238));
        bar.setBorder(BorderFactory.createLineBorder(new Color(220, 53, 69), 1));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

        JLabel label = new JLabel(message);
        label.setForeground(new Color(184, 28, 28));
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        label.setHorizontalAlignment(SwingConstants.CENTER);

        bar.add(label, BorderLayout.CENTER);
        return bar;
    }

    private JPanel createSectionPanel(String title, DefaultTableModel model) {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        container.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        container.setPreferredSize(new Dimension(0, 220));

        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)), title);
        border.setTitleFont(new Font("SansSerif", Font.BOLD, 13));
        container.setBorder(border);

        JTable table = new JTable(model);
        table.setRowHeight(28);
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