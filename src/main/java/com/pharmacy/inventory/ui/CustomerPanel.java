package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.CustomerDAO;
import com.pharmacy.inventory.model.Customer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class CustomerPanel extends JPanel {
    private final CustomerDAO customerDAO;
    private JTable customerTable;
    private DefaultTableModel tableModel;
    private JTextField searchField = new JTextField(20);

    public CustomerPanel(CustomerDAO customerDAO) {
        this.customerDAO = customerDAO;

        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(245, 246, 250));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // --- HEADER SECTION ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Patient & Customer Management");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Search and Actions
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setOpaque(false);

        searchField.setPreferredSize(new Dimension(200, 35));
        searchField.setToolTipText("Search by Name or MRN...");

        JButton searchBtn = new JButton("Search");
        JButton refreshBtn = new JButton("Refresh List");
        JButton addBtn = new JButton("+ Add New Patient");
        addBtn.setBackground(new Color(46, 204, 113));
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);

        actionPanel.add(new JLabel("Quick Find: "));
        actionPanel.add(searchField);
        actionPanel.add(searchBtn);
        actionPanel.add(refreshBtn);
        actionPanel.add(addBtn);

        headerPanel.add(actionPanel, BorderLayout.SOUTH);

        // --- TABLE SECTION ---
        String[] columns = {"ID", "First Name", "Last Name", "MRN (Medical Record #)", "Phone", "Email"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        customerTable = new JTable(tableModel);
        customerTable.setRowHeight(35);
        customerTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        customerTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(customerTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 221, 225)));

        // --- LAYOUT ASSEMBLY ---
        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // --- LISTENERS ---
        refreshBtn.addActionListener(e -> loadData());
        searchBtn.addActionListener(e -> performSearch());
        searchField.addActionListener(e -> performSearch());
        addBtn.addActionListener(e -> showAddCustomerDialog());

        loadData(); // Initial load
    }

    public void loadData() {
        tableModel.setRowCount(0);
        List<Customer> customers = customerDAO.getAllCustomers();
        for (Customer c : customers) {
            // We skip ID 0 (Walk-in) to keep the management panel for real patients
            if (c.getCustomerId() > 0) {
                tableModel.addRow(new Object[]{
                        c.getCustomerId(),
                        c.getFirstName(),
                        c.getLastName(),
                        c.getMedicalRecordNumber(),
                        c.getPhone(),
                        c.getEmail()
                });
            }
        }
    }


    public void refreshData() {
        loadData();
    }

    private void performSearch() {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            loadData();
            return;
        }

        tableModel.setRowCount(0);
        List<Customer> customers = customerDAO.getAllCustomers();
        for (Customer c : customers) {
            boolean matches = (c.getFirstName() != null && c.getFirstName().toLowerCase().contains(query)) ||
                    (c.getLastName() != null && c.getLastName().toLowerCase().contains(query)) ||
                    (c.getMedicalRecordNumber() != null && c.getMedicalRecordNumber().toLowerCase().contains(query));

            if (matches && c.getCustomerId() > 0) {
                tableModel.addRow(new Object[]{
                        c.getCustomerId(), c.getFirstName(), c.getLastName(),
                        c.getMedicalRecordNumber(), c.getPhone(), c.getEmail()
                });
            }
        }
    }

    private void showAddCustomerDialog() {
        JTextField fName = new JTextField();
        JTextField lName = new JTextField();
        JTextField phone = new JTextField();
        JTextField email = new JTextField();
        JTextField mrn = new JTextField();

        Object[] message = {
                "First Name:", fName,
                "Last Name:", lName,
                "Phone Number:", phone,
                "Email Address:", email,
                "Medical Record Number:", mrn
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Register New Patient", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION && !fName.getText().trim().isEmpty()) {
            Customer c = new Customer();
            c.setFirstName(fName.getText().trim());
            c.setLastName(lName.getText().trim());
            c.setPhone(phone.getText().trim());
            c.setEmail(email.getText().trim());
            c.setMedicalRecordNumber(mrn.getText().trim());

            if (customerDAO.saveCustomer(c)) {
                JOptionPane.showMessageDialog(this, "Patient Registered Successfully.");
                loadData();
            } else {
                JOptionPane.showMessageDialog(this, "Error: Could not save patient.");
            }
        }
    }
}