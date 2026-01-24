package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.*;
import com.pharmacy.inventory.model.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;


public class UserManagementPanel extends JPanel {
    private JTextField nameField = new JTextField();
    private JTextField emailField = new JTextField();
    private JPasswordField passField = new JPasswordField();
    private JComboBox<String> roleCombo = new JComboBox<>(new String[]{"admin", "pharmacist", "cashier", "manager"});
    private JTable userTable;
    private DefaultTableModel tableModel;
    private UserDAO userDAO;

    public UserManagementPanel(UserDAO userDAO) {
        this.userDAO = userDAO;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // LEFT SIDE: Form
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setPreferredSize(new Dimension(300, 0));

        addFormField(formPanel, "Full Name", nameField);
        addFormField(formPanel, "Email Address", emailField);
        addFormField(formPanel, "Initial Password", passField);

        JLabel rLabel = new JLabel("System Role");
        rLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(rLabel);
        roleCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        roleCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        formPanel.add(roleCombo);

        formPanel.add(Box.createVerticalStrut(20));
        JButton addBtn = new JButton("CREATE USER ACCOUNT");
        addBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        addBtn.setBackground(new Color(46, 204, 113));
        addBtn.setForeground(Color.WHITE);
        formPanel.add(addBtn);

        // RIGHT SIDE: Table
        String[] cols = {"ID", "Name", "Email", "Role"};
        tableModel = new DefaultTableModel(cols, 0);
        userTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(userTable);

        // Action Buttons for Table
        JPanel tableActions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton deleteBtn = new JButton("Delete Selected");
        deleteBtn.setBackground(new Color(231, 76, 60));
        deleteBtn.setForeground(Color.WHITE);
        tableActions.add(deleteBtn);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(scrollPane, BorderLayout.CENTER);
        rightPanel.add(tableActions, BorderLayout.SOUTH);

        // Assembly
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formPanel, rightPanel);
        splitPane.setDividerLocation(320);
        add(splitPane, BorderLayout.CENTER);

        // Listeners
        addBtn.addActionListener(e -> handleAdd());
        deleteBtn.addActionListener(e -> handleDelete());

        refreshTable();
    }

    private void addFormField(JPanel p, String label, JTextField f) {
        JLabel l = new JLabel(label);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(l);
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        p.add(f);
        p.add(Box.createVerticalStrut(10));
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (User u : userDAO.getAllUsers()) {
            tableModel.addRow(new Object[]{u.getUserId(), u.getName(), u.getEmail(), u.getRole()});
        }
    }

    private void handleAdd() {
        // 1. Collect data from the UI fields
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passField.getPassword());
        String role = (String) roleCombo.getSelectedItem();

        // 2. Validate that fields aren't empty
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required!", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 3. Create the User object (ID is 0 because the DB handles auto-increment)
        User newUser = new User(0, name, email, password, role);

        // 4. Pass the object to the DAO
        if (userDAO.addUser(newUser)) {
            JOptionPane.showMessageDialog(this, "User added successfully!");
            refreshTable();
            clearFields();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to add user. Check if the email already exists.", "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDelete() {
        int row = userTable.getSelectedRow();
        if (row != -1) {
            long id = (long) tableModel.getValueAt(row, 0);
            if (userDAO.deleteUser(id)) refreshTable();
        }
    }

    private void clearFields() {
        nameField.setText(""); emailField.setText(""); passField.setText("");
    }
}
