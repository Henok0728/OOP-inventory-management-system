package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.ItemDAO;
import com.pharmacy.inventory.dao.BatchDAO;
import com.pharmacy.inventory.dao.SalesDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import javax.swing.*;
import java.awt.*;

@Component
public class Inventory {
    @Autowired private ItemDAO itemDAO;
    @Autowired private BatchDAO batchDAO;
    @Autowired private SalesDAO salesDAO;
    private static BatchDetailsPanel batchDetailView;

    private JFrame frame;
    private static JPanel mainContent;
    private static CardLayout cardLayout;

    @PostConstruct
    public void init() {
        SwingUtilities.invokeLater(this::prepareGUI);
    }

    private void prepareGUI() {
        frame = new JFrame("Pharmacy Management System v1.0");
        frame.setSize(1350, 850);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        cardLayout = new CardLayout();
        mainContent = new JPanel(cardLayout);

        // Initialize Specialized Panels
        HomePanel homePage = new HomePanel(salesDAO, itemDAO, batchDAO);
        ProductsPanel productsPage = new ProductsPanel(itemDAO);
        SalesPanel salesPage = new SalesPanel(salesDAO, itemDAO); // Add this after HomePanel

// Inside mainContent.add() section
        mainContent.add(salesPage, "Sales");

// Inside createSidebar() menuItems array
        String[] menuItems = {"Home", "Products", "Sales", "Stock"}; // Ensure "Sales" is there

        JScrollPane homeScroll = new JScrollPane(homePage);
        homeScroll.setBorder(null);
        homeScroll.getVerticalScrollBar().setUnitIncrement(16);

        batchDetailView = new BatchDetailsPanel(batchDAO);

        mainContent.add(batchDetailView, "BatchDetails");
        mainContent.add(homeScroll, "Home");
        mainContent.add(productsPage, "Products");

        // UI Components
        frame.add(createHeader(), BorderLayout.NORTH);
        frame.add(createSidebar(), BorderLayout.WEST);
        frame.add(mainContent, BorderLayout.CENTER);

        cardLayout.show(mainContent, "Home");

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(52, 73, 94));
        header.setPreferredSize(new Dimension(0, 50));
        JLabel title = new JLabel("  PHARMACY OS");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        header.add(title, BorderLayout.WEST);
        return header;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new GridLayout(10, 1, 5, 5));
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBackground(new Color(45, 52, 54));

        String[] menuItems = {"Home", "Products", "Sales", "Stock"};
        for (String item : menuItems) {
            JButton btn = new JButton(item);
            btn.setFocusPainted(false);
            btn.addActionListener(e -> cardLayout.show(mainContent, item));
            sidebar.add(btn);
        }
        return sidebar;
    }

    public static void showBatchPanel(int itemId, String itemName) {
        batchDetailView.loadBatches(itemId, itemName);
        cardLayout.show(mainContent, "BatchDetails");
    }

    public static void showPage(String pageName) {
        cardLayout.show(mainContent, pageName);
    }
}