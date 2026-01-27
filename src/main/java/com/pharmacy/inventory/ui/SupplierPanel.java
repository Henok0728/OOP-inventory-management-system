package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.SupplierDAO;
import com.pharmacy.inventory.dao.PurchaseDAO;
import com.pharmacy.inventory.model.Supplier;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class SupplierPanel extends JPanel {
    private final SupplierDAO supplierDAO;
    private final PurchaseDAO purchaseDAO;
    private JTextField searchField;
    private JTable table;

    public SupplierPanel(SupplierDAO supplierDAO, PurchaseDAO purchaseDAO) {
        this.supplierDAO = supplierDAO;
        this.purchaseDAO = purchaseDAO;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(245, 245, 245));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        initializeUI();
    }

    private void initializeUI() {
        this.removeAll();

        // --- SECTION 1: KPI CARDS (PO & GRN Focus) ---
        JPanel kpiContainer = new JPanel(new GridLayout(1, 4, 15, 0));
        kpiContainer.setOpaque(false);
        kpiContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));


        kpiContainer.add(createKpiCard("Total Suppliers",
                String.valueOf(supplierDAO.getTotalSuppliersCount()), new Color(0, 123, 255)));

        kpiContainer.add(createKpiCard("Active Partners",
                String.valueOf(supplierDAO.getActiveSuppliersCount()), new Color(40, 167,  69)));

        kpiContainer.add(createKpiCard("POs Awaiting Approval",
                String.valueOf(purchaseDAO.getAwaitingApprovalCount()), new Color(255, 193, 7)));

        kpiContainer.add(createKpiCard("Pending GRN Receipts",
                String.valueOf(purchaseDAO.getAwaitingDeliveryCount()), new Color(108, 117, 125)));

        add(kpiContainer);
        add(Box.createRigidArea(new Dimension(0, 20)));


        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

        searchField = new JTextField(20);
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                updateTableSearch();
            }
        });

        JPanel searchContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        searchContainer.setOpaque(false);
        searchContainer.add(new JLabel("🔍 Search Suppliers:  "));
        searchContainer.add(searchField);
        headerPanel.add(searchContainer, BorderLayout.WEST);

        JButton addBtn = new JButton("+ Register Supplier");
        addBtn.setBackground(new Color(40, 167, 69));
        addBtn.setForeground(Color.WHITE);
        addBtn.setPreferredSize(new Dimension(160, 35));
        addBtn.addActionListener(e -> showAddSupplierDialog());
        headerPanel.add(addBtn, BorderLayout.EAST);

        add(headerPanel);
        add(Box.createRigidArea(new Dimension(0, 15)));


        add(createSectionPanel("📋 Supplier Directory", supplierDAO.getAllSuppliersModel()));

        add(Box.createVerticalGlue());
        this.revalidate();
        this.repaint();
    }

    private void updateTableSearch() {
        String query = searchField.getText();
        table.setModel(supplierDAO.getSupplierTableModel(supplierDAO.searchSuppliers(query)));
    }

    private void showAddSupplierDialog() {
        JTextField name = new JTextField();
        JTextField contact = new JTextField();
        JTextField phone = new JTextField();
        JTextField email = new JTextField();

        Object[] message = {
                "Supplier Name:", name,
                "Contact Person:", contact,
                "Phone Number:", phone,
                "Email Address:", email
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Register New Supplier", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            Supplier s = new Supplier();
            s.setName(name.getText());
            s.setContact(contact.getText());
            s.setPhoneNumber(phone.getText());
            s.setEmail(email.getText());

            if (supplierDAO.addSupplier(s)) {
                JOptionPane.showMessageDialog(this, "Supplier added successfully!");
                refreshData();
            } else {
                JOptionPane.showMessageDialog(this, "Error: Could not save supplier.");
            }
        }
    }

    private JPanel createSectionPanel(String title, DefaultTableModel model) {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        container.setMaximumSize(new Dimension(Integer.MAX_VALUE, 600));

        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)), title);
        border.setTitleFont(new Font("SansSerif", Font.BOLD, 14));
        container.setBorder(border);

        table = new JTable(model);
        table.setRowHeight(35);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        JScrollPane scroll = new JScrollPane(table);
        container.add(scroll, BorderLayout.CENTER);
        return container;
    }

    private JPanel createKpiCard(String title, String value, Color accent) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel tL = new JLabel(title.toUpperCase());
        tL.setForeground(new Color(120, 120, 120));
        tL.setFont(new Font("SansSerif", Font.BOLD, 10));

        JLabel vL = new JLabel(value);
        vL.setFont(new Font("SansSerif", Font.BOLD, 26));

        JPanel bar = new JPanel();
        bar.setPreferredSize(new Dimension(4, 0));
        bar.setBackground(accent);

        card.add(bar, BorderLayout.WEST);
        card.add(tL, BorderLayout.NORTH);
        card.add(vL, BorderLayout.CENTER);
        return card;
    }

    public void refreshData() {
        initializeUI();
    }
}