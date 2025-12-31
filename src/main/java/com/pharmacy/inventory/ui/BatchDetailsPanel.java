package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.BatchDAO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class BatchDetailsPanel extends JPanel {
    private JTable batchTable;
    private JLabel headerLabel;
    private BatchDAO batchDAO;

    public BatchDetailsPanel(BatchDAO batchDAO) {
        this.batchDAO = batchDAO;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        headerLabel = new JLabel("Batches for: Select an Item");
        headerLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        add(headerLabel, BorderLayout.NORTH);

        batchTable = new JTable();
        add(new JScrollPane(batchTable), BorderLayout.CENTER);

        JButton backBtn = new JButton("← Back to Products");
        backBtn.addActionListener(e -> Inventory.showPage("Products"));
        add(backBtn, BorderLayout.SOUTH);
    }

    // Inside BatchDetailsPanel.java
    public void loadBatches(int itemId, String itemName) {
        headerLabel.setText("Inventory Batches for: " + itemName);

        // Use the new Model method here
        DefaultTableModel model = batchDAO.getBatchesByItemIdModel(itemId);
        batchTable.setModel(model);
    }
}