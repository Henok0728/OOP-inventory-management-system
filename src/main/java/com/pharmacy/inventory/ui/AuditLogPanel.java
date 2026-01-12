package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.AuditDAO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class AuditLogPanel extends JPanel {
    private final AuditDAO auditDAO;
    private JTable table;

    public AuditLogPanel(AuditDAO auditDAO) {
        this.auditDAO = auditDAO;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("System Audit Trail");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        add(title, BorderLayout.NORTH);

        table = new JTable();
        table.setRowHeight(30);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new TitledBorder("Recent Activities"));
        add(scroll, BorderLayout.CENTER);

        JButton refreshBtn = new JButton("Refresh Logs");
        refreshBtn.addActionListener(e -> refreshData());
        add(refreshBtn, BorderLayout.SOUTH);

        refreshData();
    }

    public void refreshData() {
        table.setModel(auditDAO.getAllLogs());
    }
}