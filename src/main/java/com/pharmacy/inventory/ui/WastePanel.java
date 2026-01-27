package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.WasteDAO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class WastePanel extends JPanel {
    private final WasteDAO wasteDAO;
    private JTable table;

    public WastePanel(WasteDAO wasteDAO) {
        this.wasteDAO = wasteDAO;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title Section
        JLabel title = new JLabel("Waste Management & Loss Logs");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        add(title, BorderLayout.NORTH);

        // Table Section
        table = new JTable();
        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Control Section
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshBtn = new JButton("↻ Refresh Waste Log");
        refreshBtn.setBackground(new Color(231, 76, 60));
        refreshBtn.setForeground(Color.WHITE);

        refreshBtn.addActionListener(e -> refreshData());
        controls.add(refreshBtn);
        add(controls, BorderLayout.SOUTH);

        // Initial Load
        refreshData();
    }

    public void refreshData() {
        SwingUtilities.invokeLater(() -> {
            table.setModel(wasteDAO.getWasteLogs());
        });
    }
}