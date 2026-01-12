package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.CustomerDAO;
import com.pharmacy.inventory.dao.ItemDAO;
import com.pharmacy.inventory.dao.SalesDAO;
import com.pharmacy.inventory.dao.AuditDAO; // Added AuditDAO
import com.pharmacy.inventory.model.Customer;
import com.pharmacy.inventory.util.UserSession;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class SalesPanel extends JPanel {
    private final SalesDAO salesDAO;
    private final ItemDAO itemDAO;
    private final CustomerDAO customerDAO;
    private final AuditDAO auditDAO; // Added AuditDAO

    private JTable cartTable;
    private DefaultTableModel cartModel;
    private JTextField barcodeSearchF = new JTextField();
    private JLabel totalLabel = new JLabel("0.00 ETB");
    private JComboBox<String> paymentMethodCombo = new JComboBox<>(new String[]{"cash", "card", "insurance"});
    private JComboBox<Customer> customerCombo = new JComboBox<>();

    private double grandTotal = 0.0;

    // Updated Constructor
    public SalesPanel(SalesDAO salesDAO, ItemDAO itemDAO, CustomerDAO customerDAO, AuditDAO auditDAO) {
        this.salesDAO = salesDAO;
        this.itemDAO = itemDAO;
        this.customerDAO = customerDAO;
        this.auditDAO = auditDAO;

        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(240, 242, 245));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- NORTH: SEARCH & CUSTOMER ---
        JPanel topPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        topPanel.setOpaque(false);

        JPanel searchBox = new JPanel(new BorderLayout(5, 5));
        searchBox.setBorder(new TitledBorder("1. Product Search"));
        barcodeSearchF.setFont(new Font("SansSerif", Font.BOLD, 18));
        searchBox.add(barcodeSearchF, BorderLayout.CENTER);

        JPanel customerBox = new JPanel(new BorderLayout(5, 5));
        customerBox.setBorder(new TitledBorder("2. Patient (Optional)"));
        customerCombo.setPreferredSize(new Dimension(150, 30));
        customerBox.add(customerCombo, BorderLayout.CENTER);

        JPanel custButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        custButtons.setOpaque(false);
        JButton addCustBtn = new JButton("+");
        JButton refreshCustBtn = new JButton("↻");
        custButtons.add(addCustBtn);
        custButtons.add(refreshCustBtn);
        customerBox.add(custButtons, BorderLayout.EAST);

        topPanel.add(searchBox);
        topPanel.add(customerBox);

        // --- CENTER: CART TABLE ---
        String[] cols = {"Item ID", "Product Name", "Batch ID", "Qty", "Unit Price", "Subtotal"};
        cartModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return column == 3; }
        };
        cartTable = new JTable(cartModel);
        cartTable.setRowHeight(35);
        setupTableAppearance();

        // --- EAST: CHECKOUT PANEL ---
        JPanel checkoutPanel = new JPanel();
        checkoutPanel.setLayout(new BoxLayout(checkoutPanel, BoxLayout.Y_AXIS));
        checkoutPanel.setPreferredSize(new Dimension(300, 0));
        checkoutPanel.setBorder(new TitledBorder("3. Checkout"));
        checkoutPanel.setBackground(Color.WHITE);

        totalLabel.setFont(new Font("Monospaced", Font.BOLD, 32));
        totalLabel.setForeground(new Color(46, 204, 113));
        totalLabel.setOpaque(true);
        totalLabel.setBackground(Color.BLACK);
        totalLabel.setHorizontalAlignment(SwingConstants.CENTER);
        totalLabel.setMaximumSize(new Dimension(280, 80));

        paymentMethodCombo.setMaximumSize(new Dimension(280, 40));

        JButton checkoutBtn = new JButton("CONFIRM SALE");
        checkoutBtn.setBackground(new Color(52, 152, 219));
        checkoutBtn.setForeground(Color.WHITE);
        checkoutBtn.setFont(new Font("SansSerif", Font.BOLD, 18));
        checkoutBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        checkoutBtn.setMaximumSize(new Dimension(280, 60));

        checkoutPanel.add(Box.createVerticalStrut(10));
        checkoutPanel.add(new JLabel("GRAND TOTAL:"));
        checkoutPanel.add(totalLabel);
        checkoutPanel.add(Box.createVerticalStrut(20));
        checkoutPanel.add(new JLabel("Payment Method:"));
        checkoutPanel.add(paymentMethodCombo);
        checkoutPanel.add(Box.createVerticalGlue());
        checkoutPanel.add(checkoutBtn);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(cartTable), BorderLayout.CENTER);
        add(checkoutPanel, BorderLayout.EAST);

        // --- LISTENERS ---
        barcodeSearchF.addActionListener(e -> processSearch());
        addCustBtn.addActionListener(e -> showQuickAddCustomerDialog());
        refreshCustBtn.addActionListener(e -> loadCustomers());
        checkoutBtn.addActionListener(e -> performCheckout());

        cartModel.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE && e.getColumn() == 3) {
                updateRowSubtotal(e.getFirstRow());
            }
        });

        loadCustomers();
    }

    private void performCheckout() {
        if (cartModel.getRowCount() == 0) return;

        Customer selected = (Customer) customerCombo.getSelectedItem();
        Long custId = (selected != null) ? selected.getCustomerId() : 0L;
        String custName = (selected != null) ? selected.toString() : "Walk-in";

        int confirm = JOptionPane.showConfirmDialog(this, "Process sale for " + custName + "?", "Checkout", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String method = (String) paymentMethodCombo.getSelectedItem();

        // 1. Execute DB logic
        if (salesDAO.executeSale(custId, cartModel, method, grandTotal)) {

            // 2. LOG THE ACTION (Integration with Audit Log)
            auditDAO.log("SALE_COMPLETED", "sales", null);

            // 3. Success! Print Receipt
            printReceipt(custName, cartModel, grandTotal, method);

            JOptionPane.showMessageDialog(this, "Sale Recorded Successfully!");
            cartModel.setRowCount(0);
            calculateTotal();
            customerCombo.setSelectedIndex(0);
        } else {
            JOptionPane.showMessageDialog(this, "Error: Check stock levels.");
        }
    }

    // ... printReceipt, loadCustomers, showQuickAddCustomerDialog, processSearch remain the same ...

    private void printReceipt(String custName, DefaultTableModel cart, double total, String method) {
        StringBuilder sb = new StringBuilder();
        sb.append("        PHARMACY MS        \n");
        sb.append("---------------------------\n");
        sb.append("Date: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date())).append("\n");
        sb.append("Customer: ").append(custName).append("\n");
        sb.append("Cashier: ").append(UserSession.getCurrentUser().getName()).append("\n"); // Personalized
        sb.append("Payment: ").append(method).append("\n");
        sb.append("---------------------------\n");
        sb.append(String.format("%-15s %3s %8s\n", "Item", "Qty", "Price"));

        for (int i = 0; i < cart.getRowCount(); i++) {
            String name = cart.getValueAt(i, 1).toString();
            if (name.length() > 14) name = name.substring(0, 12) + "..";
            int qty = Integer.parseInt(cart.getValueAt(i, 3).toString());
            double sub = (double) cart.getValueAt(i, 5);
            sb.append(String.format("%-15s %3d %8.2f\n", name, qty, sub));
        }

        sb.append("---------------------------\n");
        sb.append(String.format("TOTAL:          ETB %.2f\n", total));
        sb.append("---------------------------\n");
        sb.append("   Keep your receipt.   \n");
        sb.append("   Thank you!   \n");

        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 8));
        try {
            boolean done = textArea.print();
        } catch (Exception e) {
            System.err.println("Print Error: " + e.getMessage());
        }
    }

    private void loadCustomers() {
        customerCombo.removeAllItems();
        Customer walkIn = new Customer(); walkIn.setCustomerId(0L); walkIn.setFirstName("Walk-in Customer");
        customerCombo.addItem(walkIn);
        List<Customer> list = customerDAO.getAllCustomers();
        for (Customer c : list) customerCombo.addItem(c);
        customerCombo.setSelectedIndex(0);
    }

    private void showQuickAddCustomerDialog() {
        JTextField fName = new JTextField(); JTextField lName = new JTextField(); JTextField mrn = new JTextField();
        Object[] msg = {"First Name:", fName, "Last Name:", lName, "MRN:", mrn};
        int opt = JOptionPane.showConfirmDialog(this, msg, "Quick Register", JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION && !fName.getText().trim().isEmpty()) {
            Customer c = new Customer();
            c.setFirstName(fName.getText().trim());
            c.setLastName(lName.getText().trim());
            c.setMedicalRecordNumber(mrn.getText().trim());
            if (customerDAO.saveCustomer(c)) {
                auditDAO.log("QUICK_CUSTOMER_ADD", "customers", null);
                loadCustomers();
                customerCombo.setSelectedItem(c);
            }
        }
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

    private void addResultToCart(DefaultTableModel res, int row) {
        try {
            int id = (int) res.getValueAt(row, 0);
            String name = res.getValueAt(row, 1).toString();
            double price = Double.parseDouble(res.getValueAt(row, 4).toString());
            for (int i = 0; i < cartModel.getRowCount(); i++) {
                if (cartModel.getValueAt(i, 0).equals(id)) {
                    int qty = Integer.parseInt(cartModel.getValueAt(i, 3).toString()) + 1;
                    cartModel.setValueAt(qty, i, 3); updateRowSubtotal(i); return;
                }
            }
            int batchId = itemDAO.getAnyAvailableBatch(id);
            if (batchId != -1) {
                cartModel.addRow(new Object[]{id, name, batchId, 1, price, price});
                calculateTotal();
            } else JOptionPane.showMessageDialog(this, "Out of Stock!");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateRowSubtotal(int r) {
        try {
            int q = Integer.parseInt(cartModel.getValueAt(r, 3).toString());
            double p = (double) cartModel.getValueAt(r, 4);
            cartModel.setValueAt(q * p, r, 5); calculateTotal();
        } catch (Exception e) {}
    }

    private void calculateTotal() {
        grandTotal = 0;
        for (int i = 0; i < cartModel.getRowCount(); i++) grandTotal += (double) cartModel.getValueAt(i, 5);
        totalLabel.setText(String.format("%.2f ETB", grandTotal));
    }

    private void showSelectionDialog(DefaultTableModel res) {
        JTable t = new JTable(res);
        if (JOptionPane.showConfirmDialog(this, new JScrollPane(t), "Select Item", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION && t.getSelectedRow() != -1)
            addResultToCart(res, t.getSelectedRow());
    }

    private void setupTableAppearance() {
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);
        cartTable.getColumnModel().getColumn(3).setCellRenderer(center);
        cartTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean isS, boolean hasF, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, isS, hasF, r, c);
                setForeground(new Color(39, 174, 96)); setFont(getFont().deriveFont(Font.BOLD)); return comp;
            }
        });
    }
}