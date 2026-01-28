package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.UserDAO;
import com.pharmacy.inventory.model.User;
import com.pharmacy.inventory.util.UserSession;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginFrame extends JFrame {
    private JTextField emailField = new JTextField();
    private JPasswordField passField = new JPasswordField();
    private UserDAO userDAO;
    private Inventory inventory;

    public LoginFrame(UserDAO userDAO, Inventory inventory) {
        this.userDAO = userDAO;
        this.inventory = inventory;

        setTitle("Pharmacy Inventory Management System");
        setSize(500, 550);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Main Background
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(245, 247, 250));

        // Header Panel (Blue Section)
        JPanel header = new JPanel();
        header.setBackground(new Color(41, 128, 185));
        header.setPreferredSize(new Dimension(0, 150));
        header.setLayout(new GridBagLayout());
        JLabel welcome = new JLabel("Phramacy Inventory Management System ");
        welcome.setForeground(Color.WHITE);
        welcome.setFont(new Font("SansSerif", Font.BOLD, 27));
        header.add(welcome);

        // Login Card
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(40, 40, 40, 40));

        addStyledLabel(card, "Email Address");
        addStyledField(card, emailField);
        card.add(Box.createVerticalStrut(20));

        addStyledLabel(card, "Password");
        addStyledField(card, passField);
        card.add(Box.createVerticalStrut(30));

        JButton loginBtn = new JButton("LOGIN TO DASHBOARD");
        loginBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        loginBtn.setBackground(new Color(41, 128, 185));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        loginBtn.setFocusPainted(false);
        loginBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.add(loginBtn);

        mainPanel.add(header, BorderLayout.NORTH);
        mainPanel.add(card, BorderLayout.CENTER);
        add(mainPanel);

        loginBtn.addActionListener(e -> handleLogin());
        passField.addActionListener(e -> handleLogin());
    }

    private void addStyledLabel(JPanel panel, String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setForeground(new Color(127, 140, 141));

        // Forces the label to the far left of the card
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(label);
        panel.add(Box.createVerticalStrut(5));
    }

    private void addStyledField(JPanel panel, JTextField field) {
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        // Forces the text field to align with the label's left edge
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(209, 216, 224), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        panel.add(field);
    }

    private void handleLogin() {
        String email = emailField.getText().trim();
        String pass = new String(passField.getPassword());
        User user = userDAO.authenticate(email, pass);
        if (user != null) {
            UserSession.login(user);
            inventory.prepareGUI();
            this.dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Invalid email or password \n y=try again.", "Auth Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}