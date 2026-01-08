package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.SupplierDAO; // Ensure this DAO exists
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class SupplierPanel extends JPanel {
    private final SupplierDAO supplierDAO;

    public SupplierPanel(SupplierDAO supplierDAO) {
        this.supplierDAO = supplierDAO;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(245, 245, 245));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        initializeUI();
    }

    private void initializeUI() {
        this.removeAll();

        // --- SECTION 1: TOP KPI CARDS ---
        JPanel kpiContainer = new JPanel(new GridLayout(1, 4, 15, 0));
        kpiContainer.setOpaque(false);
        kpiContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        // Total Suppliers
        int totalSuppliers = supplierDAO.getTotalSuppliersCount();
        kpiContainer.add(createKpiCard("Total Suppliers", String.valueOf(totalSuppliers), new Color(0, 123, 255)));

        // Active Suppliers (assuming a status column exists)
        int activeSuppliers = supplierDAO.getActiveSuppliersCount();
        kpiContainer.add(createKpiCard("Active Partners", String.valueOf(activeSuppliers), new Color(40, 167, 69)));

        // Pending Orders/Invoices (Placeholder example)
        kpiContainer.add(createKpiCard("Pending Invoices", "5", new Color(255, 193, 7)));

        add(kpiContainer);
        add(Box.createRigidArea(new Dimension(0, 20)));

        // --- SECTION 2: SUPPLIER DIRECTORY TABLE ---
        add(createSectionPanel("📋 Supplier Directory",
                supplierDAO.getAllSuppliersModel()));

        add(Box.createVerticalGlue());

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
        // Made this larger to fit the supplier list comfortably
        container.setMaximumSize(new Dimension(Integer.MAX_VALUE, 500));
        container.setPreferredSize(new Dimension(0, 400));

        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)), title);
        border.setTitleFont(new Font("SansSerif", Font.BOLD, 14));
        container.setBorder(border);

        JTable table = new JTable(model);
        table.setRowHeight(35); // Slightly taller rows for readability
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        JScrollPane scroll = new JScrollPane(table);
        container.add(scroll, BorderLayout.CENTER);
        return container;
    }

    public void refreshData() {
        initializeUI();
    }
}