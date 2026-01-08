package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.ItemDAO;
import com.pharmacy.inventory.dao.SalesDAO;
import com.pharmacy.inventory.model.Item;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class SalesPanel extends JPanel {
    private final SalesDAO salesDAO;
    private final ItemDAO itemDAO;

    private JTable cartTable;
    private DefaultTableModel cartModel;
    private JTextField barcodeSearchF = new JTextField();
    private JLabel totalLabel = new JLabel("$0.00");
    private JComboBox<String> paymentMethodCombo = new JComboBox<>(new String[]{"cash", "card", "insurance"});

    private double grandTotal = 0.0;

    public SalesPanel(SalesDAO salesDAO, ItemDAO itemDAO) {
        this.salesDAO = salesDAO;
        this.itemDAO = itemDAO;
        setLayout(new BorderLayout(10, 10));

        // --- TOP: Search Area ---
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchPanel.setBorder(new TitledBorder("Search by Name, Brand, or Barcode"));
        barcodeSearchF.setFont(new Font("SansSerif", Font.BOLD, 18));
        searchPanel.add(barcodeSearchF, BorderLayout.CENTER);

        JButton addBtn = new JButton("Search / Add");
        searchPanel.add(addBtn, BorderLayout.EAST);

        // --- CENTER: Cart Table ---
        String[] cols = {"Item ID", "Name", "Batch ID", "Qty", "Unit Price", "Subtotal"};
        cartModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3; // Only Quantity is editable
            }
        };
        cartTable = new JTable(cartModel);
        cartTable.setRowHeight(30);

        // --- RIGHT: Checkout Panel ---
        JPanel checkoutPanel = new JPanel(new GridLayout(7, 1, 10, 10));
        checkoutPanel.setPreferredSize(new Dimension(280, 0));
        checkoutPanel.setBorder(new TitledBorder("Checkout Summary"));

        totalLabel.setFont(new Font("Monospaced", Font.BOLD, 30));
        totalLabel.setForeground(new Color(46, 204, 113));
        totalLabel.setHorizontalAlignment(SwingConstants.CENTER);
        totalLabel.setOpaque(true);
        totalLabel.setBackground(Color.BLACK);

        JButton checkoutBtn = new JButton("CONFIRM SALE");
        checkoutBtn.setBackground(new Color(46, 204, 113));
        checkoutBtn.setForeground(Color.WHITE);
        checkoutBtn.setFont(new Font("SansSerif", Font.BOLD, 16));

        JButton removeBtn = new JButton("Remove Selected");
        removeBtn.setBackground(new Color(231, 76, 60));
        removeBtn.setForeground(Color.WHITE);

        checkoutPanel.add(new JLabel("GRAND TOTAL:", SwingConstants.CENTER));
        checkoutPanel.add(totalLabel);
        checkoutPanel.add(new JSeparator());
        checkoutPanel.add(new JLabel("Payment Method:"));
        checkoutPanel.add(paymentMethodCombo);
        checkoutPanel.add(removeBtn);
        checkoutPanel.add(checkoutBtn);

        add(searchPanel, BorderLayout.NORTH);
        add(new JScrollPane(cartTable), BorderLayout.CENTER);
        add(checkoutPanel, BorderLayout.EAST);

        // --- Listeners ---
        addBtn.addActionListener(e -> processSearch());
        barcodeSearchF.addActionListener(e -> processSearch());
        removeBtn.addActionListener(e -> removeSelectedItem());
        checkoutBtn.addActionListener(e -> performCheckout());

        cartModel.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE && e.getColumn() == 3) {
                updateRowSubtotal(e.getFirstRow());
            }
        });
    }

    private void processSearch() {
        String input = barcodeSearchF.getText().trim();
        if (input.isEmpty()) return;

        // Use your existing searchItems method (Partial Search)
        DefaultTableModel searchResults = itemDAO.searchItems(input);

        if (searchResults.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No product found matching: " + input);
        } else if (searchResults.getRowCount() == 1) {
            // Perfect match - Add immediately
            addResultToCart(searchResults, 0);
        } else {
            // Multiple results - Show Selection Dialog
            showSelectionDialog(searchResults);
        }
        barcodeSearchF.setText("");
        barcodeSearchF.requestFocus();
    }

    private void showSelectionDialog(DefaultTableModel results) {
        JTable selectionTable = new JTable(results);
        selectionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        int choice = JOptionPane.showConfirmDialog(this, new JScrollPane(selectionTable),
                "Multiple Items Found - Select One", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (choice == JOptionPane.OK_OPTION && selectionTable.getSelectedRow() != -1) {
            addResultToCart(results, selectionTable.getSelectedRow());
        }
    }

    private void addResultToCart(DefaultTableModel results, int row) {
        try {
            // Based on your searchItems schema: ID is index 0, Name is index 1, Price is index 4
            int itemId = Integer.parseInt(results.getValueAt(row, 0).toString());
            String name = results.getValueAt(row, 1).toString();
            double price = Double.parseDouble(results.getValueAt(row, 4).toString());

            // Check if already in cart
            for (int i = 0; i < cartModel.getRowCount(); i++) {
                if (cartModel.getValueAt(i, 0).toString().equals(String.valueOf(itemId))) {
                    int qty = Integer.parseInt(cartModel.getValueAt(i, 3).toString()) + 1;
                    cartModel.setValueAt(qty, i, 3);
                    updateRowSubtotal(i);
                    return;
                }
            }

            // Find valid batch
            int batchId = itemDAO.getAnyAvailableBatch(itemId);
            if (batchId != -1) {
                cartModel.addRow(new Object[]{itemId, name, batchId, 1, price, price});
                calculateTotal();
            } else {
                JOptionPane.showMessageDialog(this, "Out of Stock for: " + name);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error adding to cart: " + e.getMessage());
        }
    }

    private void updateRowSubtotal(int row) {
        try {
            int qty = Integer.parseInt(cartModel.getValueAt(row, 3).toString());
            double price = Double.parseDouble(cartModel.getValueAt(row, 4).toString());
            cartModel.setValueAt(qty * price, row, 5);
            calculateTotal();
        } catch (Exception e) {}
    }

    private void calculateTotal() {
        grandTotal = 0;
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            grandTotal += Double.parseDouble(cartModel.getValueAt(i, 5).toString());
        }
        totalLabel.setText("$" + String.format("%.2f", grandTotal));
    }

    private void removeSelectedItem() {
        int row = cartTable.getSelectedRow();
        if (row != -1) {
            cartModel.removeRow(row);
            calculateTotal();
        }
    }

    private void performCheckout() {
        if (cartModel.getRowCount() == 0) return;

        int confirm = JOptionPane.showConfirmDialog(this, "Confirm Sale?", "Checkout", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String method = (String) paymentMethodCombo.getSelectedItem();
        boolean allSuccess = true;

        for (int i = 0; i < cartModel.getRowCount(); i++) {
            int itemId = Integer.parseInt(cartModel.getValueAt(i, 0).toString());
            int batchId = Integer.parseInt(cartModel.getValueAt(i, 2).toString());
            int qty = Integer.parseInt(cartModel.getValueAt(i, 3).toString());
            double price = Double.parseDouble(cartModel.getValueAt(i, 4).toString());

            if (!salesDAO.processSale(0, itemId, batchId, qty, price, method)) {
                allSuccess = false;
                break;
            }
        }

        if (allSuccess) {
            JOptionPane.showMessageDialog(this, "Sale Completed!");
            cartModel.setRowCount(0);
            calculateTotal();
        } else {
            JOptionPane.showMessageDialog(this, "Transaction failed. Please check inventory.");
        }
    }
}