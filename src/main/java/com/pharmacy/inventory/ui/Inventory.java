package com.pharmacy.inventory.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pharmacy.inventory.dao.BatchDAO;
import com.pharmacy.inventory.dao.CustomerDAO;
import com.pharmacy.inventory.dao.ItemDAO;
import com.pharmacy.inventory.dao.SalesDAO;
import com.pharmacy.inventory.dao.SupplierDAO;

import jakarta.annotation.PostConstruct;


@Component
public class Inventory {
    @Autowired private ItemDAO itemDAO;
    @Autowired private BatchDAO batchDAO;
    @Autowired private SalesDAO salesDAO;
    @Autowired private SupplierDAO supplierDAO;
    @Autowired private CustomerDAO customerDAO;

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
        homePage = new HomePanel(salesDAO, itemDAO, batchDAO);
        ProductsPanel productsPage = new ProductsPanel(itemDAO);
        SalesPanel salesPage = new SalesPanel(salesDAO, itemDAO, customerDAO);
        supplierPage = new SupplierPanel(supplierDAO);
        stockPage = new StockEntryPanel(batchDAO, itemDAO, supplierDAO);
        customerPage = new CustomerPanel(customerDAO);
        historyPage = new SalesHistoryPanel(salesDAO);
        batchPage = new BatchPanel(batchDAO,supplierDAO,itemDAO);

        JScrollPane homeScroll = new JScrollPane(homePage);
        homeScroll.setBorder(null);
        homeScroll.getVerticalScrollBar().setUnitIncrement(16);

        batchDetailView = new BatchDetailsPanel(batchDAO);

        mainContent.add(batchDetailView, "BatchDetails");
        mainContent.add(homeScroll, "Home");
        mainContent.add(productsPage, "Products");
        mainContent.add(salesPage, "Sales");
        mainContent.add(supplierPage, "Suppliers");
        mainContent.add(batchPage,"Batches");
        mainContent.add(stockPage, "Stock");
        mainContent.add(customerPage, "Customers");
        mainContent.add(historyPage, "History");
        

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

        String[] menuItems = {"Home", "Products", "Sales", "Suppliers","Batches","Stock","Customers", "History"};
        for (String item : menuItems) {
            JButton btn = new JButton(item);
            btn.setFocusPainted(false);
            btn.addActionListener(e -> {
                // Show the page
                cardLayout.show(mainContent, item);

                // If the button clicked was "Home", refresh the data!
                if (item.equals("Home")) {
                    homePage.refreshData();
                }
                if (item.equals("Suppliers")) {
                    supplierPage.refreshData();
                }
                if (item.equals("Stock")) {
                    stockPage.refreshData();
                }
                if (item.equals("Batches")){
                    batchPage.refreshData();
                }
                if (item.equals("Customers")) {
                    customerPage.loadData();
                }
                if (item.equals("History")) {
                    historyPage.refreshData();
                }
            });
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