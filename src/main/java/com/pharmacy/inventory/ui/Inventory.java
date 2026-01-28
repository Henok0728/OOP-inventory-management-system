package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.*;
import com.pharmacy.inventory.service.RFIDService;
import com.pharmacy.inventory.util.UserSession;
import com.pharmacy.inventory.ui.NotificationManager;
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
    @Autowired private WasteDAO wasteDAO;
    @Autowired private ReportDAO reportDAO; // Added ReportDAO
    @Autowired private PurchaseDAO purchaseDAO;

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
    private WastePanel wastePage;
    private UserManagementPanel userManagementPage;
    private ReportPanel reportPage;
    private PurchaseOrderPanel purchaseOrderPage;
    @PostConstruct
    public void init() {

        try {
            com.formdev.flatlaf.FlatLightLaf.setup();

            // Global Typography & Shapes
            UIManager.put("defaultFont", new Font("Inter", Font.PLAIN, 14));
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);

            // Table Styling (Modern, No Monospaced)
            UIManager.put("Table.font", new Font("Inter", Font.PLAIN, 13));
            UIManager.put("Table.alternateRowColor", new Color(248, 249, 250));
            UIManager.put("Table.rowHeight", 35);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.gridColor", new Color(230, 230, 230));
            UIManager.put("Table.selectionBackground", new Color(52, 152, 219, 40));
            UIManager.put("Table.selectionForeground", Color.BLACK);

            // Scrollbars
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("ScrollBar.thumbArc", 999);

        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf");
        }

        SwingUtilities.invokeLater(this::showLogin);
    }

    private void showLogin() {
        LoginFrame login = new LoginFrame(userDAO, this);
        login.setVisible(true);
    }

    public void prepareGUI() {

        frame = new JFrame("Pharmacy Management System V1.5");
        frame.setSize(1350, 850);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        cardLayout = new CardLayout();
        mainContent = new JPanel(cardLayout);

        // Initialize Specialized Panels
        homePage = new HomePanel(salesDAO, itemDAO, batchDAO);
        ProductsPanel productsPage = new ProductsPanel(itemDAO, auditDAO);
        SalesPanel salesPage = new SalesPanel(salesDAO, itemDAO, customerDAO, auditDAO);
        supplierPage = new SupplierPanel(supplierDAO, purchaseDAO);
        stockPage = new StockEntryPanel(batchDAO, itemDAO, supplierDAO, purchaseDAO, auditDAO);
        customerPage = new CustomerPanel(customerDAO);
        historyPage = new SalesHistoryPanel(salesDAO);
        batchPage = new BatchPanel(batchDAO, supplierDAO, itemDAO);
        auditPage = new AuditLogPanel(auditDAO);
        wastePage = new WastePanel(wasteDAO);
        userManagementPage = new UserManagementPanel(userDAO);
        reportPage = new ReportPanel(reportDAO);
        purchaseOrderPage = new PurchaseOrderPanel(purchaseDAO, supplierDAO, auditDAO);
        

        JScrollPane homeScroll = new JScrollPane(homePage);
        homeScroll.setBorder(null);
        homeScroll.getVerticalScrollBar().setUnitIncrement(16);

        batchDetailView = new BatchDetailsPanel(batchDAO);

        // Add to CardLayout
        mainContent.add(batchDetailView, "BatchDetails");
        mainContent.add(homeScroll, "Home");
        mainContent.add(userManagementPage, "Manage Users");
        mainContent.add(productsPage, "Products");
        mainContent.add(salesPage, "Sales");
        mainContent.add(supplierPage, "Suppliers");
        mainContent.add(stockPage, "Stock");
        mainContent.add(purchaseOrderPage, "Purchase Orders");
        mainContent.add(customerPage, "Customers");
        mainContent.add(historyPage, "History");
        mainContent.add(auditPage, "Audit");
        mainContent.add(wastePage, "Waste");
        mainContent.add(reportPage, "Reports");
       


        frame.add(createHeader(), BorderLayout.NORTH);
        frame.add(createSidebar(), BorderLayout.WEST);
        frame.add(mainContent, BorderLayout.CENTER);
        
        cardLayout.show(mainContent, "Home");
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setResizable(false);
        // --- WASTE NOTIFICATION LOGIC ---
        if (!UserSession.getUserRole().equals("cashier")) {
            Timer timer = new Timer(1500, e -> {
                NotificationManager.checkExpirations(wasteDAO, frame, UserSession.getCurrentUser().getUserId());
            });
            timer.setRepeats(false);
            timer.start();
        }

        //
        RFIDService rfidService = new RFIDService(userDAO, auditDAO);
        rfidService.start();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(52, 73, 94));
        header.setPreferredSize(new Dimension(0, 50));

        JLabel title = new JLabel(" PHARMACY INVENTORY MANAGEMENT SYSTEM Built by Team Arada");
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

        if (role.equals("admin")){
            addSidebarButton(sidebar, "Manage Users");
        }

        addSidebarButton(sidebar, "Products");
        addSidebarButton(sidebar, "Sales");
        addSidebarButton(sidebar, "Customers");

        if (role.equals("admin") || role.equals("pharmacist") || role.equals("manager")) {
            addSidebarButton(sidebar, "Stock");
            addSidebarButton(sidebar, "Purchase Orders");
            addSidebarButton(sidebar, "Suppliers");
            addSidebarButton(sidebar, "Waste");
        }

        if (role.equals("admin") || role.equals("manager")) {
            addSidebarButton(sidebar, "History");
            addSidebarButton(sidebar, "Reports");
            addSidebarButton(sidebar, "Audit");
        }
        addSidebarButton(sidebar,"About us");
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
        if (item.equals("Purchase Orders")) purchaseOrderPage.refreshData();
        if (item.equals("Customers")) customerPage.loadData();
        if (item.equals("History")) historyPage.refreshData();
        if (item.equals("Batches")) batchPage.refreshData();
        if (item.equals("Audit")) auditPage.refreshData();
        if (item.equals("Waste")) wastePage.refreshData();
        if (item.equals("Reports")) reportPage.refreshData(); // Integrated refresh logic
    }

    public static void showBatchPanel(int itemId, String itemName) {
        batchDetailView.loadBatches(itemId, itemName);
        cardLayout.show(mainContent, "BatchDetails");
    }

    public static void showPage(String pageName) {
        if (cardLayout != null && mainContent != null) {
            cardLayout.show(mainContent, pageName);
        }
    }
}