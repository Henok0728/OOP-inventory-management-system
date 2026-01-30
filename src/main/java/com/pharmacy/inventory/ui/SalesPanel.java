package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.*;
import com.pharmacy.inventory.model.Customer;
import com.pharmacy.inventory.util.UserSession;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;

public class SalesPanel extends JPanel {
    private final SalesDAO salesDAO;
    private final ItemDAO itemDAO;
    private final CustomerDAO customerDAO;
    private final AuditDAO auditDAO;

    private JTable cartTable;
    private DefaultTableModel cartModel;
    private JTextField barcodeSearchF = new JTextField();
    private JLabel totalLabel = new JLabel("0.00 ETB");
    private JComboBox<String> paymentMethodCombo = new JComboBox<>(new String[]{"cash", "card", "insurance"});
    private JComboBox<Customer> customerCombo = new JComboBox<>();

    // UI Constants
    private final Color PRIMARY_COLOR = new Color(41, 128, 185);
    private final Color SUCCESS_COLOR = new Color(39, 174, 96);
    private final Color DANGER_COLOR = new Color(192, 57, 43);
    private final Color BG_COLOR = new Color(236, 240, 241);

    private double grandTotal = 0.0;

    public SalesPanel(SalesDAO salesDAO, ItemDAO itemDAO, CustomerDAO customerDAO, AuditDAO auditDAO) {
        this.salesDAO = salesDAO;
        this.itemDAO = itemDAO;
        this.customerDAO = customerDAO;
        this.auditDAO = auditDAO;

        initLayout();
        loadCustomers();
    }

    private void initLayout() {
        setLayout(new BorderLayout(15, 15));
        setBackground(BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createCartPanel(), BorderLayout.CENTER);
        add(createCheckoutSidePanel(), BorderLayout.EAST);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new GridLayout(1, 2, 20, 0));
        header.setOpaque(false);

        // Search Section
        JPanel searchBox = new JPanel(new BorderLayout(10, 10));
        searchBox.setBackground(Color.WHITE);
        searchBox.setBorder(new TitledBorder(new LineBorder(PRIMARY_COLOR), " 1. Scan or Search Product "));

        barcodeSearchF.setFont(new Font("SansSerif", Font.PLAIN, 18));
        barcodeSearchF.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Color.LIGHT_GRAY), BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        barcodeSearchF.addActionListener(e -> processSearch());
        searchBox.add(barcodeSearchF, BorderLayout.CENTER);

        // Customer Section
        JPanel customerBox = new JPanel(new BorderLayout(10, 10));
        customerBox.setBackground(Color.WHITE);
        customerBox.setBorder(new TitledBorder(new LineBorder(PRIMARY_COLOR), " 2. Patient / Customer "));

        customerCombo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        customerBox.add(customerCombo, BorderLayout.CENTER);

        JPanel custActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        custActions.setOpaque(false);
        JButton addCustBtn = createStyledButton("+ New", PRIMARY_COLOR);
        JButton refreshBtn = createStyledButton("↻", Color.GRAY);

        addCustBtn.addActionListener(e -> showQuickAddCustomerDialog());
        refreshBtn.addActionListener(e -> loadCustomers());

        custActions.add(addCustBtn);
        custActions.add(refreshBtn);
        customerBox.add(custActions, BorderLayout.EAST);

        header.add(searchBox);
        header.add(customerBox);
        return header;
    }

    private JPanel createCartPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);

        String[] cols = {"Item ID", "Product Name", "Qty", "Unit Price", "Subtotal", "Max Stock"};
        cartModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return c == 2; } // Only Qty can be edited
        };

        cartTable = new JTable(cartModel);
        cartTable.setRowHeight(40);
        cartTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
        cartTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        cartTable.setSelectionBackground(new Color(232, 244, 253));
        setupTableAppearance();

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolbar.setOpaque(false);

        JButton btnDeleteSelected = createStyledButton("Remove Selected", DANGER_COLOR);
        JButton btnClearAll = createStyledButton("Clear All", Color.DARK_GRAY);

        btnDeleteSelected.addActionListener(e -> removeSelectedItems());
        btnClearAll.addActionListener(e -> clearWholeCart());

        toolbar.add(btnDeleteSelected);
        toolbar.add(btnClearAll);

        centerPanel.add(toolbar, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        cartModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 2) {
                updateRowSubtotal(e.getFirstRow());
            }
        });

        return centerPanel;
    }

    private JPanel createCheckoutSidePanel() {
        JPanel side = new JPanel(new GridBagLayout());
        side.setBackground(Color.WHITE);
        side.setPreferredSize(new Dimension(320, 0));
        side.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Color.LIGHT_GRAY), BorderFactory.createEmptyBorder(25, 20, 25, 20)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;


        JLabel title = new JLabel("CHECKOUT SUMMARY");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 0; gbc.insets = new Insets(0, 0, 40, 0);
        side.add(title, gbc);


        JLabel totalHeader = new JLabel("TOTAL");
        totalHeader.setFont(new Font("SansSerif", Font.BOLD, 14));
        totalHeader.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 1; gbc.insets = new Insets(0, 0, 10, 0);
        side.add(totalHeader, gbc);


        totalLabel.setFont(new Font("Monospaced", Font.BOLD, 36));
        totalLabel.setForeground(SUCCESS_COLOR);
        totalLabel.setBackground(new Color(33, 33, 33));
        totalLabel.setOpaque(true);
        totalLabel.setHorizontalAlignment(SwingConstants.CENTER);
        totalLabel.setPreferredSize(new Dimension(280, 100));
        totalLabel.setMinimumSize(new Dimension(280, 100));
        gbc.gridy = 2; gbc.insets = new Insets(0, 0, 40, 0);
        side.add(totalLabel, gbc);

        // Payment mode header
        JLabel payHeader = new JLabel("PAYMENT MODE");
        payHeader.setFont(new Font("SansSerif", Font.BOLD, 14));
        payHeader.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 3; gbc.insets = new Insets(0, 0, 10, 0);
        side.add(payHeader, gbc);

        paymentMethodCombo.setFont(new Font("SansSerif", Font.BOLD, 16));
        paymentMethodCombo.setPreferredSize(new Dimension(280, 45));
        gbc.gridy = 4; gbc.insets = new Insets(0, 0, 40, 0);
        side.add(paymentMethodCombo, gbc);

        // Spacer to push button down
        gbc.gridy = 5; gbc.weighty = 1.0;
        side.add(Box.createVerticalGlue(), gbc);

        // Checkout button
        JButton checkoutBtn = new JButton("CONFIRM SALE");
        checkoutBtn.setBackground(SUCCESS_COLOR);
        checkoutBtn.setForeground(Color.WHITE);
        checkoutBtn.setFont(new Font("SansSerif", Font.BOLD, 22));
        checkoutBtn.setPreferredSize(new Dimension(280, 80));
        checkoutBtn.addActionListener(e -> performCheckout()); // Button Listener
        gbc.gridy = 6; gbc.weighty = 0; gbc.insets = new Insets(20, 0, 0, 0);
        side.add(checkoutBtn, gbc);

        return side;
    }

    private void performCheckout() {
        if (cartModel.getRowCount() == 0) return;

        Customer selected = (Customer) customerCombo.getSelectedItem();
        String paymentMethod = (String) paymentMethodCombo.getSelectedItem();

        int confirm = JOptionPane.showConfirmDialog(this, "Process sale for " + selected + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        if (salesDAO.executeSale(selected.getCustomerId(), cartModel, paymentMethod, grandTotal)) {
            auditDAO.log("SALE_COMPLETED", "sales", null);

            printReceipt(selected.toString(), cartModel, grandTotal, paymentMethod);

            JOptionPane.showMessageDialog(this, "Sale Completed Successfully!");
            cartModel.setRowCount(0);
            calculateTotal();
        } else {
            JOptionPane.showMessageDialog(this, "Transaction failed: Check stock availability.");
        }
    }

    private void printReceipt(String custName, DefaultTableModel cart, double total, String method) {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String dateStr = sdf.format(new Date());

        sb.append("      CITY CENTER PHARMACY      \n");
        sb.append("    5- KILO street, Addis Ababa  \n");
        sb.append("================================\n");
        sb.append(String.format("DATE:    %s\n", dateStr));
        sb.append(String.format("PATIENT: %s\n", custName));
        sb.append(String.format("CASHIER: %s\n", UserSession.getCurrentUser().getName()));
        sb.append(String.format("PAYMENT: %s\n", method.toUpperCase()));
        sb.append("--------------------------------\n");
        sb.append(String.format("%-14s %5s %11s\n", "ITEM", "QTY", "PRICE"));
        sb.append("--------------------------------\n");

        for (int i = 0; i < cart.getRowCount(); i++) {
            String name = cart.getValueAt(i, 1).toString();
            if (name.length() > 14) name = name.substring(0, 12) + "..";
            int qty = Integer.parseInt(cart.getValueAt(i, 2).toString());
            double sub = (double) cart.getValueAt(i, 4);
            sb.append(String.format("%-14s %5d %11.2f\n", name, qty, sub));
        }

        sb.append("--------------------------------\n");
        sb.append(String.format("TOTAL:           ETB %11.2f\n", total));
        sb.append("================================\n");
        sb.append("   Thank you for choosing us!   \n");
        sb.append("      Get Well Soon!            \n");
        sb.append("\n\n\n\n");

        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 9));
        try {
            textArea.print();
        } catch (Exception e) {
            System.err.println("Print Error: " + e.getMessage());
        }
    }

    private void removeSelectedItems() {
        int[] selectedRows = cartTable.getSelectedRows();
        for (int i = selectedRows.length - 1; i >= 0; i--) {
            cartModel.removeRow(selectedRows[i]);
        }
        calculateTotal();
    }

    private void clearWholeCart() {
        if (cartModel.getRowCount() == 0) return;
        if (JOptionPane.showConfirmDialog(this, "Clear cart?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            cartModel.setRowCount(0);
            calculateTotal();
        }
    }

    private void calculateTotal() {
        grandTotal = 0;
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            grandTotal += (double) cartModel.getValueAt(i, 4);
        }
        totalLabel.setText(String.format("%.2f ETB", grandTotal));
    }

    private void updateRowSubtotal(int r) {
        try {
            int q = Integer.parseInt(cartModel.getValueAt(r, 2).toString());
            int max = (int) cartModel.getValueAt(r, 5);
            if (q > max) {
                JOptionPane.showMessageDialog(this, "Only " + max + " available.");
                q = max;
                cartModel.setValueAt(q, r, 2);
            }
            double price = (double) cartModel.getValueAt(r, 3);
            cartModel.setValueAt(q * price, r, 4);
            calculateTotal();
        } catch (Exception e) { cartModel.setValueAt(1, r, 2); }
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        btn.setFocusPainted(false);
        return btn;
    }

    private void setupTableAppearance() {
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        cartTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);

        cartTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean isS, boolean hasF, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, isS, hasF, r, c);
                comp.setForeground(SUCCESS_COLOR);
                comp.setFont(comp.getFont().deriveFont(Font.BOLD));
                return comp;
            }
        });

        cartTable.getColumnModel().removeColumn(cartTable.getColumnModel().getColumn(5));
        cartTable.getColumnModel().removeColumn(cartTable.getColumnModel().getColumn(0));
    }

    private void processSearch() {
        String input = barcodeSearchF.getText().trim();
        if (input.isEmpty()) return;
        DefaultTableModel res = itemDAO.searchItems(input);
        if (res.getRowCount() == 0) JOptionPane.showMessageDialog(this, "Product not found.");
        else if (res.getRowCount() == 1) addResultToCart(res, 0);
        else showSelectionDialog(res);
        barcodeSearchF.setText("");
    }


    public void remoteBarcodeScanned(String code) {
        if (code == null || code.isEmpty()) return;

        SwingUtilities.invokeLater(() -> {
            String cleanBarcode = code.replaceAll("[^0-9]", "");
            barcodeSearchF.setText(cleanBarcode);
            processSearch();
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.toFront();
            }

            System.out.println("Barcode Processed: " + cleanBarcode);
        });
    }

    private void addResultToCart(DefaultTableModel res, int row) {
        int id = (int) res.getValueAt(row, 0);
        String name = res.getValueAt(row, 1).toString();
        double price = Double.parseDouble(res.getValueAt(row, 4).toString());
        int stock = Integer.parseInt(res.getValueAt(row, 5).toString());

        if (stock <= 0) {
            JOptionPane.showMessageDialog(this, "Out of stock!");
            return;
        }

        for (int i = 0; i < cartModel.getRowCount(); i++) {
            if ((int) cartModel.getValueAt(i, 0) == id) {
                int qty = Integer.parseInt(cartModel.getValueAt(i, 2).toString());
                cartModel.setValueAt(qty + 1, i, 2);
                updateRowSubtotal(i);
                return;
            }
        }
        cartModel.addRow(new Object[]{id, name, 1, price, price, stock});
        calculateTotal();
    }

    private void showSelectionDialog(DefaultTableModel res) {
        JTable t = new JTable(res);
        t.setRowHeight(30);
        if (JOptionPane.showConfirmDialog(this, new JScrollPane(t), "Select Product", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION && t.getSelectedRow() != -1)
            addResultToCart(res, t.getSelectedRow());
    }

    private void loadCustomers() {
        customerCombo.removeAllItems();
        Customer walkIn = new Customer();
        walkIn.setCustomerId(0L);
        walkIn.setFirstName("Walk-in Customer");
        walkIn.setLastName("");
        customerCombo.addItem(walkIn);
        customerDAO.getAllCustomers().forEach(customerCombo::addItem);
        customerCombo.setSelectedIndex(0);
    }

    private void showQuickAddCustomerDialog() {
        JTextField fName = new JTextField(); JTextField lName = new JTextField(); JTextField mrn = new JTextField();
        Object[] msg = {"First Name:", fName, "Last Name:", lName, "MRN:", mrn};
        if (JOptionPane.showConfirmDialog(this, msg, "New Patient", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            Customer c = new Customer();
            c.setFirstName(fName.getText());
            c.setLastName(lName.getText());
            c.setMedicalRecordNumber(mrn.getText());
            if (customerDAO.saveCustomer(c)) { loadCustomers(); customerCombo.setSelectedItem(c); }
        }
    }
}