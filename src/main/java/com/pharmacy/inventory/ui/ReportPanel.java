package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.ReportDAO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class ReportPanel extends JPanel {
    private final ReportDAO reportDAO;
    private JPanel cardsPanel;
    private DefaultTableModel salesModel, purchaseModel;

    public ReportPanel(ReportDAO reportDAO) {
        this.reportDAO = reportDAO;
        setLayout(new BorderLayout(20, 20));
        setBackground(new Color(245, 246, 250));
        setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        cardsPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        cardsPanel.setOpaque(false);

        JPanel tablesPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        tablesPanel.setOpaque(false);

        salesModel = new DefaultTableModel(new String[]{"Date", "Customer", "Method", "Amount"}, 0);
        tablesPanel.add(createTableSection("Recent Customer Sales", salesModel));

        purchaseModel = new DefaultTableModel(new String[]{"Date", "Supplier/Item", "Batch", "Cost"}, 0);
        tablesPanel.add(createTableSection("Recent Supplier Purchases", purchaseModel));

        add(cardsPanel, BorderLayout.NORTH);
        add(tablesPanel, BorderLayout.CENTER);

        refreshData();
    }

    private JPanel createTableSection(String title, DefaultTableModel model) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 16));

        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setEnabled(false); // Make table read-only
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        panel.add(lbl, BorderLayout.NORTH);
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }

    public void refreshData() {
        cardsPanel.removeAll();
        Map<String, Double> stats = reportDAO.getFinancialSummary();

        cardsPanel.add(createStatCard("REVENUE", stats.get("REVENUE"), new Color(41, 128, 185)));
        cardsPanel.add(createStatCard("EXPENSES (COGS)", stats.get("COGS"), new Color(230, 126, 34)));
        cardsPanel.add(createStatCard("WASTE LOSS", stats.get("WASTE"), new Color(192, 57, 43)));
        cardsPanel.add(createStatCard("NET PROFIT", stats.get("PROFIT"), new Color(39, 174, 96)));

        updateTableModel(salesModel, reportDAO.getRecentSalesSummary());
        updateTableModel(purchaseModel, reportDAO.getRecentPurchasesSummary());

        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    private void updateTableModel(DefaultTableModel model, List<Object[]> data) {
        model.setRowCount(0);
        for (Object[] row : data) model.addRow(row);
    }

    private JPanel createStatCard(String title, Double val, Color color) {
        double amount = (val != null) ? val : 0.0;
        JPanel card = new JPanel(new GridLayout(2, 1));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(color, 2));

        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JLabel lblValue = new JLabel(String.format("%.2f ETB", amount), SwingConstants.CENTER);
        lblValue.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblValue.setForeground(color);

        card.add(lblTitle);
        card.add(lblValue);
        return card;
    }
}