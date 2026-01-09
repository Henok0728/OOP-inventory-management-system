package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.SalesDAO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class SalesHistoryPanel extends JPanel {
    private final SalesDAO salesDAO;
    private JTable masterTable;
    private JTable detailTable;
    private DefaultTableModel detailModel;

    public SalesHistoryPanel(SalesDAO salesDAO) {
        this.salesDAO = salesDAO;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // --- TOP: MASTER TABLE (All Sales) ---
        masterTable = new JTable();
        masterTable.setRowHeight(30);
        masterTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // --- BOTTOM: DETAIL TABLE (Items in selected sale) ---
        String[] detailCols = {"Item Name", "Qty", "Price", "Subtotal"};
        detailModel = new DefaultTableModel(detailCols, 0);
        detailTable = new JTable(detailModel);
        detailTable.setRowHeight(25);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                createViewPanel(masterTable, "Transaction Logs"),
                createViewPanel(detailTable, "Items in Selected Sale"));
        splitPane.setDividerLocation(300);

        // Header with Refresh
        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Sales & Transaction History");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        JButton refreshBtn = new JButton("Refresh Data");
        header.add(title, BorderLayout.WEST);
        header.add(refreshBtn, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        // Listeners
        refreshBtn.addActionListener(e -> refreshData());

        masterTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && masterTable.getSelectedRow() != -1) {
                int saleId = (int) masterTable.getValueAt(masterTable.getSelectedRow(), 0);
                loadSaleDetails(saleId);
            }
        });

        refreshData();
    }

    private JPanel createViewPanel(JTable table, String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new TitledBorder(title));
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    public void refreshData() {
        masterTable.setModel(salesDAO.getSalesHistory());
        detailModel.setRowCount(0);
    }

    private void loadSaleDetails(int saleId) {
        detailTable.setModel(salesDAO.getSaleItems(saleId));
    }
}