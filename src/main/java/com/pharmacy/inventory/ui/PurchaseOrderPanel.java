package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.*;
import com.pharmacy.inventory.model.Supplier;
import com.pharmacy.inventory.model.Item;
import com.pharmacy.inventory.util.UserSession;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PurchaseOrderPanel extends JPanel {
    private final PurchaseDAO purchaseDAO;
    private final SupplierDAO supplierDAO;
    private final ItemDAO itemDAO;
    private final AuditDAO auditDAO;

    private JComboBox<Supplier> supplierCombo = new JComboBox<>();
    private JComboBox<Item> itemCombo = new JComboBox<>();
    private JTextField qtyF = new JTextField(5);
    private JTextField unitPriceF = new JTextField(7);
    private JLabel totalDisplayLabel = new JLabel("Basket Total: $0.00");

    private JTable basketTable;
    private DefaultTableModel basketTableModel;
    private List<Object[]> basketItems = new ArrayList<>();

    private JTable requestsTable;
    private DefaultTableModel requestsTableModel;
    private JTable activeTable;
    private DefaultTableModel activeTableModel;
    private JTable historyTable;
    private DefaultTableModel historyTableModel;

    public PurchaseOrderPanel(PurchaseDAO pDAO, SupplierDAO sDAO, ItemDAO iDAO, AuditDAO aDAO) {
        this.purchaseDAO = pDAO;
        this.supplierDAO = sDAO;
        this.itemDAO = iDAO;
        this.auditDAO = aDAO;

        setLayout(new BorderLayout());
        initializeUI();
    }

    private void initializeUI() {
        JTabbedPane tabbedPane = new JTabbedPane();

        // --- MAIN MANAGE TAB ---
        JPanel managePanel = new JPanel(new BorderLayout());

        // 1. Top Section: The Order Creation Form
        JPanel creationSection = new JPanel(new BorderLayout(5, 5));
        creationSection.add(createSupplierPanel(), BorderLayout.WEST);
        creationSection.add(createBasketPanel(), BorderLayout.CENTER);
        creationSection.setPreferredSize(new Dimension(0, 250)); // Fixed height for form

        // 2. Bottom Section: The Dual Tables (Inbox & Active)
        JPanel tablesSection = createTablesSection();

        // Use a SplitPane to prevent overflow and allow user resizing
        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, creationSection, tablesSection);
        mainSplit.setDividerLocation(250);
        managePanel.add(mainSplit, BorderLayout.CENTER);

        // --- HISTORY TAB ---
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.add(createHistoryTableSection(), BorderLayout.CENTER);

        tabbedPane.addTab("📦 Manage Orders & Approvals", managePanel);
        tabbedPane.addTab("📜 Purchase History", historyPanel);

        add(tabbedPane, BorderLayout.CENTER);
        refreshData();
    }

    private JPanel createSupplierPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("1. Supplier"));
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Select:"), gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(supplierCombo, gbc);
        return panel;
    }

    private JPanel createBasketPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("2. Current Basket Items"));
        panel.setBackground(Color.WHITE);

        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputRow.add(new JLabel("Item:"));
        inputRow.add(itemCombo);
        inputRow.add(new JLabel("Qty:"));
        inputRow.add(qtyF);
        inputRow.add(new JLabel("Price:"));
        inputRow.add(unitPriceF);

        JButton addBtn = new JButton("Add");
        addBtn.addActionListener(e -> addToBasket());
        inputRow.add(addBtn);

        basketTableModel = new DefaultTableModel(new String[]{"ID", "Item Name", "Qty", "Price", "Subtotal"}, 0);
        basketTable = new JTable(basketTableModel);

        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.add(totalDisplayLabel, BorderLayout.WEST);
        JButton saveBtn = new JButton("Submit PO Request");
        saveBtn.setBackground(new Color(0, 123, 255));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.addActionListener(e -> handleBulkOrder());
        bottomRow.add(saveBtn, BorderLayout.EAST);

        panel.add(inputRow, BorderLayout.NORTH);
        panel.add(new JScrollPane(basketTable), BorderLayout.CENTER);
        panel.add(bottomRow, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createTablesSection() {
        // Horizontal split for Approval Inbox vs Active Orders
        JPanel container = new JPanel(new GridLayout(1, 2, 10, 0));

        // LEFT: Approval Inbox
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("🔔 Admin Inbox (Requested)"));
        requestsTableModel = new DefaultTableModel(new String[]{"ID", "Supplier", "Total", "By"}, 0);
        requestsTable = new JTable(requestsTableModel);
        leftPanel.add(new JScrollPane(requestsTable), BorderLayout.CENTER);

        if (UserSession.getUserRole().equalsIgnoreCase("admin") || UserSession.getUserRole().equalsIgnoreCase("manager")) {
            JPanel btnPanel = new JPanel(new GridLayout(1, 2, 5, 5));
            JButton rejectBtn = new JButton("Reject");
            rejectBtn.setBackground(new Color(220, 53, 69));
            rejectBtn.setForeground(Color.WHITE);

            JButton approveBtn = new JButton("Approve");
            approveBtn.setBackground(new Color(40, 167, 69));
            approveBtn.setForeground(Color.WHITE);

            approveBtn.addActionListener(e -> {
                int row = requestsTable.getSelectedRow();
                if (row != -1) {
                    long id = (long) requestsTable.getValueAt(row, 0);
                    if (purchaseDAO.approveOrder(id)) refreshData();
                }
            });
            rejectBtn.addActionListener(e -> {
                int row = requestsTable.getSelectedRow();
                if (row != -1) {
                    long id = (long) requestsTable.getValueAt(row, 0);
                    if (purchaseDAO.rejectOrder(id)) refreshData();
                }
            });
            btnPanel.add(rejectBtn); btnPanel.add(approveBtn);
            leftPanel.add(btnPanel, BorderLayout.SOUTH);
        }

        // RIGHT: Active Orders
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("🚚 Approved Orders (Active)"));
        activeTableModel = new DefaultTableModel(new String[]{"ID", "Supplier", "Total", "Status"}, 0);
        activeTable = new JTable(activeTableModel);
        rightPanel.add(new JScrollPane(activeTable), BorderLayout.CENTER);

        container.add(leftPanel);
        container.add(rightPanel);
        return container;
    }

    private void addToBasket() {
        try {
            Item item = (Item) itemCombo.getSelectedItem();
            int qty = Integer.parseInt(qtyF.getText());
            double price = Double.parseDouble(unitPriceF.getText());
            if (item != null && qty > 0) {
                basketTableModel.addRow(new Object[]{item.getItem_id(), item.getName(), qty, price, (qty * price)});
                basketItems.add(new Object[]{item.getItem_id(), qty, price});
                calculateTotal();
                qtyF.setText("");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Check format!");
        }
    }

    private void calculateTotal() {
        double total = 0;
        for (int i = 0; i < basketTableModel.getRowCount(); i++) {
            total += (double) basketTableModel.getValueAt(i, 4);
        }
        totalDisplayLabel.setText(String.format("Basket Total: $%.2f", total));
    }

    private void handleBulkOrder() {
        if (basketItems.isEmpty()) return;
        Supplier s = (Supplier) supplierCombo.getSelectedItem();
        if (s == null) return;

        long userId = UserSession.getCurrentUser().getUserId();
        String role = UserSession.getUserRole();

        if (purchaseDAO.createBulkPurchaseOrder(s.getSupplierId(), basketItems, userId, role)) {
            JOptionPane.showMessageDialog(this, "Request Submitted Successfully!");
            basketTableModel.setRowCount(0);
            basketItems.clear();
            calculateTotal();
            refreshData();
        }
    }

    private JPanel createHistoryTableSection() {
        JPanel panel = new JPanel(new BorderLayout());
        historyTableModel = new DefaultTableModel(new String[]{"ID", "Supplier", "Amount", "Date"}, 0);
        historyTable = new JTable(historyTableModel);
        panel.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        return panel;
    }

    public void refreshData() {
        supplierCombo.removeAllItems();
        supplierDAO.getAllSuppliers().forEach(supplierCombo::addItem);
        itemCombo.removeAllItems();
        itemDAO.getAllItemsList().forEach(itemCombo::addItem);

        requestsTableModel.setRowCount(0);
        purchaseDAO.getOrdersByStatus("REQUESTED").forEach(requestsTableModel::addRow);

        activeTableModel.setRowCount(0);
        purchaseDAO.getActiveOrders().forEach(activeTableModel::addRow);

        historyTableModel.setRowCount(0);
        purchaseDAO.getPurchaseHistory().forEach(historyTableModel::addRow);
    }
}