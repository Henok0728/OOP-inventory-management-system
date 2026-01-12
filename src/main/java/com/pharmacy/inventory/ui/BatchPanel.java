package com.pharmacy.inventory.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import com.pharmacy.inventory.dao.BatchDAO;
import com.pharmacy.inventory.dao.ItemDAO;
import com.pharmacy.inventory.dao.SupplierDAO;
import com.pharmacy.inventory.model.Supplier;

public class  BatchPanel extends JPanel{
    private JTextField searchField;
    private JTable table;
    private final BatchDAO batchDAO;
    private final SupplierDAO supplierDAO;
    private final ItemDAO itemDAO;
    public BatchPanel(BatchDAO batchDAO,SupplierDAO supplierDAO,ItemDAO itemDAO){
        this.batchDAO = batchDAO;
        this.supplierDAO = supplierDAO;
        this.itemDAO = itemDAO;
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

        kpiContainer.add(createKpiCard("Total Suppliers", String.valueOf(supplierDAO.getTotalSuppliersCount()), new Color(0, 123, 255)));
        kpiContainer.add(createKpiCard("Active Partners", String.valueOf(supplierDAO.getActiveSuppliersCount()), new Color(40, 167, 69)));
        kpiContainer.add(createKpiCard("Pending Invoices", "0", new Color(255, 193, 7)));

        add(kpiContainer);
        add(Box.createRigidArea(new Dimension(0, 20)));

        // --- SECTION 2: SEARCH & ACTION HEADER ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        // Search Bar (Left)
        searchField = new JTextField(20);
        searchField.setToolTipText("Search by Name or Contact...");
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                updateTableSearch();
            }
        });

        JPanel searchContainer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchContainer.setOpaque(false);
        searchContainer.add(new JLabel("🔍 Search: "));
        searchContainer.add(searchField);
        headerPanel.add(searchContainer, BorderLayout.WEST);

        // Add Button (Right)
        JButton addBtn = new JButton("+ Register Supplier");
        addBtn.setBackground(new Color(40, 167, 69));
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);
        addBtn.addActionListener(e -> showAddSupplierDialog());
        headerPanel.add(addBtn, BorderLayout.EAST);

        add(headerPanel);
        add(Box.createRigidArea(new Dimension(0, 10)));

        // --- SECTION 3: SUPPLIER DIRECTORY TABLE ---
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
        // Create a simple popup form
        JTextField name = new JTextField();
        JTextField contact = new JTextField();
        JTextField phone = new JTextField();
        Object[] message = {
                "Supplier Name:", name,
                "Contact Person:", contact,
                "Phone Number:", phone
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Add New Supplier", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            Supplier s = new Supplier();
            s.setName(name.getText());
            s.setContact(contact.getText());
            s.setPhoneNumber(phone.getText());

            if (supplierDAO.addSupplier(s)) {
                refreshData();
            } else {
                JOptionPane.showMessageDialog(this, "Error saving supplier.");
            }
        }
    }

    // UPDATED: Now stores a reference to the table so search can update it
    private JPanel createSectionPanel(String title, DefaultTableModel model) {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        container.setMaximumSize(new Dimension(Integer.MAX_VALUE, 550));
        container.setPreferredSize(new Dimension(0, 450));

        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)), title);
        border.setTitleFont(new Font("SansSerif", Font.BOLD, 14));
        container.setBorder(border);

        table = new JTable(model);
        table.setRowHeight(35);
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
        JLabel tL = new JLabel(title); tL.setForeground(Color.GRAY);
        JLabel vL = new JLabel(value); vL.setFont(new Font("SansSerif", Font.BOLD, 22));
        JPanel bar = new JPanel(); bar.setPreferredSize(new Dimension(5, 0)); bar.setBackground(accent);
        card.add(bar, BorderLayout.WEST);
        card.add(tL, BorderLayout.NORTH);
        card.add(vL, BorderLayout.CENTER);
        return card;
    }

    public void refreshData() {
        initializeUI();
    }
}