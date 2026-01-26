package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.BatchDAO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class BatchDetailsPanel extends JPanel {
    private final BatchDAO batchDAO;
    private JTable table;
    private JLabel titleLabel;

    public BatchDetailsPanel(BatchDAO batchDAO) {
        this.batchDAO = batchDAO;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));


        JPanel header = new JPanel(new BorderLayout());
        titleLabel = new JLabel("Batch Details");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        JButton backBtn = new JButton("← Back to Products");

        backBtn.addActionListener(e -> Inventory.showPage("Products"));

        header.add(titleLabel, BorderLayout.WEST);
        header.add(backBtn, BorderLayout.EAST);

        table = new JTable();
        add(header, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void loadBatches(int itemId, String itemName) {
        titleLabel.setText("Batches for: " + itemName);
        DefaultTableModel model = batchDAO.getBatchesByItem(itemId);
        table.setModel(model);
    }
}