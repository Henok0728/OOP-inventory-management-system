package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.*;
import com.pharmacy.inventory.util.UserSession;
import com.pharmacy.inventory.util.NotificationManager; // Import the utility we created
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
    @Autowired private SupplierDAO supplierDAO;
    @Autowired private CustomerDAO customerDAO;
    @Autowired private UserDAO userDAO;
    @Autowired private AuditDAO auditDAO;
    @Autowired private WasteDAO wasteDAO; // <-- FIX: Add this line to solve the "cannot find symbol" error

    private static BatchDetailsPanel batchDetailView;
    private JFrame frame;
    private static JPanel mainContent;
    private static CardLayout cardLayout;

    private HomePanel homePage;
    private SupplierPanel supplierPage;
    private StockEntryPanel stockPage;
    private CustomerPanel customerPage;
    private SalesHistoryPanel historyPage;
    private BatchPanel batchPage;
    private AuditLogPanel auditPage;
    private WastePanel wastePage; // Add reference for the Waste UI

    @PostConstruct
    public void init() {
        SwingUtilities.invokeLater(this::showLogin);
    }

    private void showLogin() {
        LoginFrame login = new LoginFrame(userDAO, this);
        login.setVisible(true);
    }

    public void prepareGUI() {
        frame = new JFrame("Pharmacy Management System v1.0");
        frame.setSize(1350, 850);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        cardLayout = new CardLayout();
        mainContent = new JPanel(cardLayout);

        // Initialize Specialized Panels
        homePage = new HomePanel(salesDAO, itemDAO, batchDAO);
        ProductsPanel productsPage = new ProductsPanel(itemDAO, auditDAO);
        SalesPanel salesPage = new SalesPanel(salesDAO, itemDAO, customerDAO, auditDAO);
        supplierPage = new SupplierPanel(supplierDAO);
        stockPage = new StockEntryPanel(batchDAO, itemDAO, supplierDAO);
        customerPage = new CustomerPanel(customerDAO);
        historyPage = new SalesHistoryPanel(salesDAO);
        batchPage = new BatchPanel(batchDAO, supplierDAO, itemDAO);
        auditPage = new AuditLogPanel(auditDAO);
        wastePage = new WastePanel(wasteDAO); // Initialize the Waste Panel

        JScrollPane homeScroll = new JScrollPane(homePage);
        homeScroll.setBorder(null);
        homeScroll.getVerticalScrollBar().setUnitIncrement(16);

        batchDetailView = new BatchDetailsPanel(batchDAO);

        // Add to CardLayout
        mainContent.add(batchDetailView, "BatchDetails");
        mainContent.add(homeScroll, "Home");
        mainContent.add(productsPage, "Products");
        mainContent.add(salesPage, "Sales");
        mainContent.add(supplierPage, "Suppliers");
        mainContent.add(batchPage, "Batches");
        mainContent.add(stockPage, "Stock");
        mainContent.add(customerPage, "Customers");
        mainContent.add(historyPage, "History");
        mainContent.add(auditPage, "Audit");
        mainContent.add(wastePage, "Waste"); // Register Waste Page

        frame.add(createHeader(), BorderLayout.NORTH);
        frame.add(createSidebar(), BorderLayout.WEST);
        frame.add(mainContent, BorderLayout.CENTER);

        cardLayout.show(mainContent, "Home");
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // --- WASTE NOTIFICATION LOGIC ---
        // Trigger the check for expired items after UI is visible
        if (!UserSession.getUserRole().equals("cashier")) {
            Timer timer = new Timer(1500, e -> {
                NotificationManager.checkExpirations(wasteDAO, frame, UserSession.getCurrentUser().getUserId());
            });
            timer.setRepeats(false);
            timer.start();
        }
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(52, 73, 94));
        header.setPreferredSize(new Dimension(0, 50));

        JLabel title = new JLabel("  PHARMACY OS");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));

        String userInfo = "User: " + UserSession.getCurrentUser().getName() +
                " [" + UserSession.getUserRole().toUpperCase() + "]  ";
        JLabel userLabel = new JLabel(userInfo);
        userLabel.setForeground(new Color(189, 195, 199));

        header.add(title, BorderLayout.WEST);
        header.add(userLabel, BorderLayout.EAST);
        return header;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBackground(new Color(45, 52, 54));

        String role = UserSession.getUserRole();

        addSidebarButton(sidebar, "Home");
        addSidebarButton(sidebar, "Products");
        addSidebarButton(sidebar, "Sales");
        addSidebarButton(sidebar, "Customers");

        if (role.equals("admin") || role.equals("pharmacist") || role.equals("manager")) {
            addSidebarButton(sidebar, "Batches");
            addSidebarButton(sidebar, "Stock");
            addSidebarButton(sidebar, "Suppliers");
            addSidebarButton(sidebar, "Waste"); // Added Waste button for inventory controllers
        }

        if (role.equals("admin") || role.equals("manager")) {
            addSidebarButton(sidebar, "History");
            addSidebarButton(sidebar, "Audit");
        }

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setPreferredSize(new Dimension(180, 40));
        logoutBtn.setBackground(new Color(192, 57, 43));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.addActionListener(e -> {
            UserSession.logout();
            frame.dispose();
            showLogin();
        });
        sidebar.add(logoutBtn);

        return sidebar;
    }

    private void addSidebarButton(JPanel container, String item) {
        JButton btn = new JButton(item);
        btn.setPreferredSize(new Dimension(180, 40));
        btn.setFocusPainted(false);
        btn.addActionListener(e -> {
            cardLayout.show(mainContent, item);
            refreshPanelData(item);
        });
        container.add(btn);
    }

    private void refreshPanelData(String item) {
        if (item.equals("Home")) homePage.refreshData();
        if (item.equals("Suppliers")) supplierPage.refreshData();
        if (item.equals("Stock")) stockPage.refreshData();
        if (item.equals("Customers")) customerPage.loadData();
        if (item.equals("History")) historyPage.refreshData();
        if (item.equals("Batches")) batchPage.refreshData();
        if (item.equals("Audit")) auditPage.refreshData();
        if (item.equals("Waste")) wastePage.refreshData(); // Ensure waste refreshes
    }

    public static void showBatchPanel(int itemId, String itemName) {
        batchDetailView.loadBatches(itemId, itemName);
        cardLayout.show(mainContent, "BatchDetails");
    }

    // Add this to Inventory.java
    public static void showPage(String pageName) {
        if (cardLayout != null && mainContent != null) {
            cardLayout.show(mainContent, pageName);
        }
    }
}