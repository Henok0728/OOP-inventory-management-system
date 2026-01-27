package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.PurchaseDAO;
import com.pharmacy.inventory.dao.SupplierDAO;
import com.pharmacy.inventory.dao.AuditDAO;
import com.pharmacy.inventory.model.Supplier;
import com.pharmacy.inventory.util.UserSession;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;


public class PurchaseOrderPanel extends JPanel {
    private final PurchaseDAO purchaseDAO;
    private final SupplierDAO supplierDAO;
    private final AuditDAO auditDAO;

    private JComboBox<Supplier> supplierCombo = new JComboBox<>();
    private JTextField totalAmountF = new JTextField();

    private JTable activeTable;
    private DefaultTableModel activeTableModel;

    private JTable historyTable;
    private DefaultTableModel historyTableModel;

    public PurchaseOrderPanel(PurchaseDAO pDAO, SupplierDAO sDAO, AuditDAO aDAO) {
        this.purchaseDAO = pDAO;
        this.supplierDAO = sDAO;
        this.auditDAO = aDAO;

        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));

        initializeUI();
    }

    private void initializeUI() {
        JTabbedPane tabbedPane = new JTabbedPane();


        JPanel activePanel = new JPanel(new BorderLayout(10, 10));
        activePanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));


        activePanel.add(createInputForm(), BorderLayout.NORTH);


        activePanel.add(createActiveTableSection(), BorderLayout.CENTER);


        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        historyPanel.add(createHistoryTableSection(), BorderLayout.CENTER);

        tabbedPane.addTab("📦 Active Orders", activePanel);
        tabbedPane.addTab("📜 Purchase History", historyPanel);

        add(tabbedPane, BorderLayout.CENTER);
        refreshData();
    }

    private JPanel createInputForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("📝 Create New Purchase Order (PO)"));
        panel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Supplier:"), gbc);
        gbc.gridx = 1;
        panel.add(supplierCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Est. Total:"), gbc);
        gbc.gridx = 1;
        panel.add(totalAmountF, gbc);

        JButton saveBtn = new JButton("Authorize PO Request");
        saveBtn.setBackground(new Color(0, 123, 255));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.addActionListener(e -> handleOrder());

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        panel.add(saveBtn, gbc);

        return panel;
    }

    private JPanel createActiveTableSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);


        String[] columns = {"PO Number", "Supplier", "Amount", "Requested By", "Status"};
        activeTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        activeTable = new JTable(activeTableModel);
        activeTable.setRowHeight(30);

        panel.add(new JScrollPane(activeTable), BorderLayout.CENTER);


        String role = UserSession.getUserRole();
        if (role.equals("admin") || role.equals("manager")) {
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

            JButton rejectBtn = new JButton("Reject Request");
            rejectBtn.setBackground(new Color(220, 53, 69));
            rejectBtn.setForeground(Color.WHITE);
            rejectBtn.addActionListener(e -> {
                int row = activeTable.getSelectedRow();
                if (row != -1) onRejectClick((long) activeTable.getValueAt(row, 0));
            });

            JButton approveBtn = new JButton("Approve Order");
            approveBtn.setBackground(new Color(40, 167, 69));
            approveBtn.setForeground(Color.WHITE);
            approveBtn.addActionListener(e -> {
                int row = activeTable.getSelectedRow();
                if (row != -1) onApproveClick((long) activeTable.getValueAt(row, 0));
            });

            btnPanel.add(rejectBtn);
            btnPanel.add(approveBtn);
            panel.add(btnPanel, BorderLayout.SOUTH);
        }

        return panel;
    }

    private JPanel createHistoryTableSection() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] columns = {"PO Number", "Supplier", "Total Amount", "Date Delivered"};
        historyTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        historyTable = new JTable(historyTableModel);
        historyTable.setRowHeight(30);
        panel.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        return panel;
    }

    private void handleOrder() {
        try {
            long userId = UserSession.getCurrentUser().getUserId();
            String role = UserSession.getUserRole();
            double total = Double.parseDouble(totalAmountF.getText());
            Supplier s = (Supplier) supplierCombo.getSelectedItem();

            if (s != null && purchaseDAO.createPurchaseOrder(s.getSupplierId(), total, userId, role)) {
                JOptionPane.showMessageDialog(this, "Order Submitted!");
                totalAmountF.setText("");
                refreshData();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void onApproveClick(long id) {
        purchaseDAO.approveOrder(id);
        JOptionPane.showMessageDialog(this, "Order #" + id + " Approved.");
        refreshData();
    }

    private void onRejectClick(long id) {
        int confirm = JOptionPane.showConfirmDialog(this, "Reject Order #" + id + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION && purchaseDAO.rejectOrder(id)) {
            refreshData();
        }
    }

    public void refreshData() {
        // Refresh Supplier Dropdown
        supplierCombo.removeAllItems();
        supplierDAO.getAllSuppliers().forEach(supplierCombo::addItem);

        // Refresh Active Table (Tab 1)
        activeTableModel.setRowCount(0);
        List<Object[]> activeOrders = purchaseDAO.getActiveOrders();
        for (Object[] row : activeOrders) {
            activeTableModel.addRow(row);
        }

        // Refresh History Table (Tab 2)
        historyTableModel.setRowCount(0);
        List<Object[]> historyOrders = purchaseDAO.getPurchaseHistory();
        for (Object[] row : historyOrders) {
            historyTableModel.addRow(row);
        }
    }
}