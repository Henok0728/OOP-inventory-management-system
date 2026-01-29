package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.ItemDAO;
import com.pharmacy.inventory.dao.AuditDAO;
import com.pharmacy.inventory.model.Item;
import com.pharmacy.inventory.util.UserSession;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;

public class ProductsPanel extends JPanel {

    private final ItemDAO itemDAO;
    private final AuditDAO auditDAO;

    private JTable table;
    private DefaultTableModel tableModel;
    private Integer selectedItemId = null;

    private final JTextField nameF = new JTextField(), genericF = new JTextField(), brandF = new JTextField();
    private final JTextField barcodeF = new JTextField(), dosageF = new JTextField(), strengthF = new JTextField();
    private final JTextField priceF = new JTextField(), searchF = new JTextField(), reorderF = new JTextField();

    private final JComboBox<String> categoryCombo = new JComboBox<>(new String[]{
            "antibiotics", "painkiller", "vaccine", "medical supply", "non medical supply", "equipment"
    });
    private final JCheckBox prescriptionCheck = new JCheckBox("Prescription Required");

    public ProductsPanel(ItemDAO itemDAO, AuditDAO auditDAO) {
        this.itemDAO = itemDAO;
        this.auditDAO = auditDAO;

        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(245, 245, 245));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String role = UserSession.getUserRole();
        boolean isAdmin = role.equals("admin") || role.equals("manager");

        JPanel northPanel = new JPanel(new BorderLayout(10, 10));
        northPanel.setOpaque(false);

        JLabel headerLabel = new JLabel("Pharmacy Inventory Catalog");
        headerLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        headerLabel.setForeground(new Color(44, 62, 80));

        searchF.setPreferredSize(new Dimension(0, 50));
        searchF.setFont(new Font("SansSerif", Font.PLAIN, 14));
        searchF.setBorder(new TitledBorder(BorderFactory.createLineBorder(new Color(189, 195, 199)), "Search Database"));
        setupSearchLogic();

        northPanel.add(headerLabel, BorderLayout.NORTH);
        northPanel.add(searchF, BorderLayout.CENTER);

        table = new JTable();
        table.setRowHeight(35);
        table.setShowGrid(false);
        table.setSelectionBackground(new Color(232, 242, 255));
        table.setSelectionForeground(Color.BLACK);

        loadTableData();
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(210, 210, 210)));
        scrollPane.getViewport().setBackground(Color.WHITE);

        if (isAdmin) {
            add(createEntryForm(), BorderLayout.EAST);
        }

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setOpaque(false);

        JButton viewInventoryBtn = new JButton("📊 VIEW STOCK & BATCH HISTORY");
        viewInventoryBtn.setBackground(new Color(41, 128, 185)); // Professional Blue
        viewInventoryBtn.setForeground(Color.WHITE);
        viewInventoryBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        viewInventoryBtn.setPreferredSize(new Dimension(300, 45));
        viewInventoryBtn.setFocusPainted(false);
        viewInventoryBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        viewInventoryBtn.addActionListener(e -> {
            if (selectedItemId != null) {
                Inventory.showBatchPanel(selectedItemId, nameF.getText());
            } else {
                JOptionPane.showMessageDialog(this, "Please select an item from the table first.");
            }
        });

        actionPanel.add(viewInventoryBtn);

        add(northPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);
    }

    private JPanel createEntryForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(380, 0)); // Slightly wider for 2x2 buttons
        panel.setBorder(new TitledBorder("Manage Product Specifications"));
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 10, 4, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addFormRow(panel, "Name:", nameF, gbc, row++);
        addFormRow(panel, "Generic:", genericF, gbc, row++);
        addFormRow(panel, "Brand:", brandF, gbc, row++);
        addFormRow(panel, "Barcode:", barcodeF, gbc, row++);
        addFormRow(panel, "Category:", categoryCombo, gbc, row++);
        addFormRow(panel, "Dosage:", dosageF, gbc, row++);
        addFormRow(panel, "Strength:", strengthF, gbc, row++);
        addFormRow(panel, "Price (ETB):", priceF, gbc, row++);
        addFormRow(panel, "Reorder:", reorderF, gbc, row++);

        gbc.gridx = 1; gbc.gridy = row++;
        prescriptionCheck.setOpaque(false);
        panel.add(prescriptionCheck, gbc);

        JPanel btnGrid = new JPanel(new GridLayout(2, 2, 8, 8));
        btnGrid.setOpaque(false);

        JButton addBtn = createStyledButton("Register", new Color(41, 128, 185));
        JButton updateBtn = createStyledButton("Update", new Color(39, 174, 96));
        JButton deleteBtn = createStyledButton("Remove", new Color(192, 57, 43));
        JButton clearBtn = createStyledButton("Clear", new Color(127, 140, 141));

        btnGrid.add(addBtn);
        btnGrid.add(updateBtn);
        btnGrid.add(deleteBtn);
        btnGrid.add(clearBtn);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 10, 10, 10);
        panel.add(btnGrid, gbc);

        addBtn.addActionListener(e -> saveAction(true));
        updateBtn.addActionListener(e -> saveAction(false));
        deleteBtn.addActionListener(e -> deleteAction());
        clearBtn.addActionListener(e -> clearFields());

        return panel;
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(0, 35));
        return btn;
    }

    private void addFormRow(JPanel p, String label, JComponent comp, GridBagConstraints gbc, int row) {
        gbc.gridwidth = 1; gbc.gridx = 0; gbc.gridy = row;
        p.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        p.add(comp, gbc);
    }

    private void saveAction(boolean isNew) {
        try {
            Item item = new Item();
            item.setName(nameF.getText());
            item.setGenericName(genericF.getText());
            item.setBrandName(brandF.getText());
            item.setBarcode(barcodeF.getText());
            item.setCategory((String) categoryCombo.getSelectedItem());
            item.setDosageForm(dosageF.getText());
            item.setStrength(strengthF.getText());
            item.setRetailPrice(Double.parseDouble(priceF.getText()));
            item.setReorderLevel(Integer.parseInt(reorderF.getText()));
            item.setPrescriptionRequired(prescriptionCheck.isSelected());

            if (isNew) {
                itemDAO.addItem(item);
                auditDAO.log("REGISTER_NEW_ITEM", "items", null);
                JOptionPane.showMessageDialog(this, "Item added to system!");
            } else if (selectedItemId != null) {
                item.setItem_id(selectedItemId);
                itemDAO.updateItem(item);
                auditDAO.log("UPDATE_ITEM_SPECS", "items", selectedItemId);
                JOptionPane.showMessageDialog(this, "Item updated!");
            }
            loadTableData();
            clearFields();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Check numeric fields: " + ex.getMessage());
        }
    }

    public void loadTableData() {
        tableModel = itemDAO.getAllItems();
        table.setModel(tableModel);
        table.setDefaultRenderer(Object.class, new ProductRenderer());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                int r = table.convertRowIndexToModel(table.getSelectedRow());
                selectedItemId = (Integer) tableModel.getValueAt(r, 0);
                nameF.setText(val(r, 1));
                genericF.setText(val(r, 2));
                brandF.setText(val(r, 3));
                barcodeF.setText(val(r, 4));
                categoryCombo.setSelectedItem(val(r, 5));
                dosageF.setText(val(r, 6));
                strengthF.setText(val(r, 7));
                priceF.setText(val(r, 8));
                reorderF.setText(val(r, 9));

                if (tableModel.getColumnCount() > 10) {
                    Object rxValue = tableModel.getValueAt(r, 10);
                    boolean isRx = rxValue != null && (rxValue.toString().equals("1") || rxValue.toString().equalsIgnoreCase("true"));
                    prescriptionCheck.setSelected(isRx);
                }
            }
        });
    }

    private String val(int row, int col) {
        if (col >= tableModel.getColumnCount()) return "";
        Object o = tableModel.getValueAt(row, col);
        return o == null ? "" : o.toString();
    }

    private void deleteAction() {
        if (selectedItemId == null) return;
        int confirm = JOptionPane.showConfirmDialog(this, "Delete item?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            itemDAO.removeItem(selectedItemId);
            auditDAO.log("DELETE_ITEM", "items", selectedItemId);
            loadTableData();
            clearFields();
        }
    }

    private void clearFields() {
        JTextField[] fields = {nameF, genericF, brandF, barcodeF, dosageF, strengthF, priceF, reorderF};
        for (JTextField field : fields) field.setText("");
        categoryCombo.setSelectedIndex(0);
        prescriptionCheck.setSelected(false);
        selectedItemId = null;
        table.clearSelection();
    }

    private void setupSearchLogic() {
        searchF.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
            private void filter() {
                if (tableModel == null) return;
                TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
                table.setRowSorter(sorter);
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchF.getText()));
            }
        });
    }

    private static class ProductRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean isS, boolean hasF, int r, int c) {
            Component cell = super.getTableCellRendererComponent(t, v, isS, hasF, r, c);
            if (!isS) {
                cell.setBackground(r % 2 == 0 ? Color.WHITE : new Color(245, 245, 245));
            }
            if (t.getColumnName(c).toLowerCase().contains("prescript")) {
                boolean req = v != null && (v.toString().equals("1") || v.toString().equalsIgnoreCase("true"));
                setText(req ? "RX REQUIRED" : "OTC");
                setForeground(req ? new Color(192, 57, 43) : new Color(39, 174, 96));
                setFont(getFont().deriveFont(Font.BOLD));
            } else {
                setForeground(Color.BLACK);
            }
            return cell;
        }
    }

    public void refreshData() {
        loadTableData();
    }
}