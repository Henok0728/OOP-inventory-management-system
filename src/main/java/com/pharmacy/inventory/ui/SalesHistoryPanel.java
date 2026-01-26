package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.SalesDAO;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
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
        setBackground(new Color(240, 242, 245));

        // --- HEADER SECTION ---
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Sales & Transaction History");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setOpaque(false);

        JButton refreshBtn = new JButton("Refresh Data");
        JButton voidBtn = new JButton("Void Selected Sale");
        voidBtn.setForeground(Color.RED);
        voidBtn.setFont(new Font("SansSerif", Font.BOLD, 12));

        actionPanel.add(voidBtn);
        actionPanel.add(refreshBtn);

        header.add(title, BorderLayout.WEST);
        header.add(actionPanel, BorderLayout.EAST);

        // --- TABLES SETUP ---
        masterTable = new JTable();
        masterTable.setRowHeight(35);
        masterTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setupMasterTableAppearance();

        String[] detailCols = {"Item Name", "Qty", "Price", "Subtotal"};
        detailModel = new DefaultTableModel(detailCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        detailTable = new JTable(detailModel);
        detailTable.setRowHeight(30);

        // --- LAYOUT ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                createViewPanel(masterTable, "Transaction Logs (Recent First)"),
                createViewPanel(detailTable, "Items in Selected Transaction"));
        splitPane.setDividerLocation(350);
        splitPane.setResizeWeight(0.6);

        add(header, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        // --- LISTENERS ---
        refreshBtn.addActionListener(e -> refreshData());

        voidBtn.addActionListener(e -> {
            int row = masterTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a sale from the list first.");
                return;
            }

            int saleId = (int) masterTable.getValueAt(row, 0);
            String method = masterTable.getValueAt(row, 4).toString();

            if ("VOIDED".equalsIgnoreCase(method)) {
                JOptionPane.showMessageDialog(this, "This transaction has already been voided.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Voiding Sale #" + saleId + " will reset the total and RESTOCK all items.\nAre you sure?",
                    "Confirm Transaction Void", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                if (salesDAO.voidSale(saleId)) {
                    JOptionPane.showMessageDialog(this, "Sale #" + saleId + " has been voided and stock returned.");
                    refreshData();
                } else {
                    JOptionPane.showMessageDialog(this, "Critical error during voiding process.");
                }
            }
        });

        // Click a sale to see its items
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
        setupMasterTableAppearance();
        detailModel.setRowCount(0);
    }

    private void loadSaleDetails(int saleId) {
        detailTable.setModel(salesDAO.getSaleItems(saleId));
    }

    private void setupMasterTableAppearance() {

        masterTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean isS, boolean hasF, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, isS, hasF, r, c);


                String method = t.getValueAt(r, 4).toString();

                if ("VOIDED".equalsIgnoreCase(method)) {
                    comp.setForeground(Color.LIGHT_GRAY);
                    if (!isS) comp.setBackground(new Color(255, 240, 240)); // Pale red background
                } else {
                    comp.setForeground(Color.BLACK);
                    if (!isS) comp.setBackground(Color.WHITE);
                }

                if (isS) {
                    comp.setBackground(t.getSelectionBackground());
                    comp.setForeground(t.getSelectionForeground());
                }
                return comp;
            }
        });
    }
}