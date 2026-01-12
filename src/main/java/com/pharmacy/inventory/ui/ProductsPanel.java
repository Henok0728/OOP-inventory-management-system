package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.ItemDAO;
import com.pharmacy.inventory.dao.AuditDAO; // Ensure this is imported
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
    private final AuditDAO auditDAO; // Added AuditDAO

    private JTable table;
    private DefaultTableModel tableModel;
    private Integer selectedItemId = null;

    // Core Item Fields
    private final JTextField nameF = new JTextField(), genericF = new JTextField(), brandF = new JTextField();
    private final JTextField barcodeF = new JTextField(), dosageF = new JTextField(), strengthF = new JTextField();
    private final JTextField priceF = new JTextField(), searchF = new JTextField(), reorderF = new JTextField();

    private final JComboBox<String> categoryCombo = new JComboBox<>(new String[]{
            "antibiotics", "painkiller", "vaccine", "medical supply", "non medical supply", "equipment"
    });
    private final JCheckBox prescriptionCheck = new JCheckBox("Prescription Required");

    // Updated Constructor to include AuditDAO
    public ProductsPanel(ItemDAO itemDAO, AuditDAO auditDAO) {
        this.itemDAO = itemDAO;
        this.auditDAO = auditDAO;
        setLayout(new BorderLayout(10, 10));

        // --- SEARCH BAR (NORTH) ---
        searchF.setBorder(new TitledBorder("Search Database (Name/Barcode/Generic)"));
        setupSearchLogic();

        // --- TABLE (CENTER) ---
        table = new JTable();
        loadTableData();
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new TitledBorder("Inventory Items"));

        // --- FORM (EAST) ---
        JPanel formPanel = createEntryForm();

        add(searchF, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(formPanel, BorderLayout.EAST);
    }

    private JPanel createEntryForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(350, 0));
        panel.setBorder(new TitledBorder("Item Information"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Determine permissions
        String role = UserSession.getUserRole();
        boolean isAdmin = role.equals("admin") || role.equals("manager");

        int row = 0;
        addFormRow(panel, "Name:", nameF, gbc, row++);
        addFormRow(panel, "Generic:", genericF, gbc, row++);
        addFormRow(panel, "Brand:", brandF, gbc, row++);
        addFormRow(panel, "Barcode:", barcodeF, gbc, row++);
        addFormRow(panel, "Category:", categoryCombo, gbc, row++);
        addFormRow(panel, "Dosage:", dosageF, gbc, row++);
        addFormRow(panel, "Strength:", strengthF, gbc, row++);
        addFormRow(panel, "Retail Price (ETB):", priceF, gbc, row++);
        addFormRow(panel, "Reorder Level:", reorderF, gbc, row++);

        gbc.gridx = 1; gbc.gridy = row++;
        panel.add(prescriptionCheck, gbc);

        // CRUD Buttons
        JPanel btnPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        JButton addBtn = new JButton("Register Item");
        JButton updateBtn = new JButton("Update Item");
        JButton deleteBtn = new JButton("Remove Item");
        JButton clearBtn = new JButton("Clear Form");
        JButton viewBatchesBtn = new JButton("View Inventory");

        // Apply Security Restrictions
        addBtn.setEnabled(isAdmin);
        updateBtn.setEnabled(isAdmin);
        deleteBtn.setEnabled(isAdmin);

        // Disable fields if not admin to prevent confusion
        if (!isAdmin) {
            String msg = "Locked: View Only Access";
            nameF.setEditable(false); genericF.setEditable(false);
            priceF.setEditable(false); barcodeF.setEditable(false);
            addBtn.setToolTipText(msg);
            updateBtn.setToolTipText(msg);
            deleteBtn.setToolTipText(msg);
        }

        viewBatchesBtn.setBackground(new Color(52, 152, 219));
        viewBatchesBtn.setForeground(Color.WHITE);

        btnPanel.add(addBtn); btnPanel.add(updateBtn);
        btnPanel.add(deleteBtn); btnPanel.add(clearBtn);
        btnPanel.add(viewBatchesBtn);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 10, 10);
        panel.add(btnPanel, gbc);

        // Listeners
        addBtn.addActionListener(e -> saveAction(true));
        updateBtn.addActionListener(e -> saveAction(false));
        deleteBtn.addActionListener(e -> deleteAction());
        clearBtn.addActionListener(e -> clearFields());
        viewBatchesBtn.addActionListener(e -> {
            if (selectedItemId != null) {
                Inventory.showBatchPanel(selectedItemId, nameF.getText());
            } else {
                JOptionPane.showMessageDialog(this, "Select a product to view stock history.");
            }
        });

        return panel;
    }

    private void addFormRow(JPanel p, String label, JComponent comp, GridBagConstraints gbc, int row) {
        gbc.gridwidth = 1; gbc.gridx = 0; gbc.gridy = row;
        p.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        p.add(comp, gbc);
    }

    private void saveAction(boolean isNew) {
        if (!UserSession.getUserRole().equals("admin") && !UserSession.getUserRole().equals("manager")) {
            JOptionPane.showMessageDialog(this, "Unauthorized: Only administrators can modify items.");
            return;
        }

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
                auditDAO.log("REGISTER_NEW_ITEM", "items", null); // Logging the creation
                JOptionPane.showMessageDialog(this, "New item registered in catalog!");
            } else if (selectedItemId != null) {
                item.setItem_id(selectedItemId);
                itemDAO.updateItem(item);
                auditDAO.log("UPDATE_ITEM_SPECS", "items", selectedItemId); // Logging the update
                JOptionPane.showMessageDialog(this, "Item specifications updated!");
            }

            loadTableData();
            clearFields();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Retail Price and Reorder Level must be numeric.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    public void loadTableData() {
        tableModel = itemDAO.getAllItems();
        table.setModel(tableModel);
        table.setRowHeight(30);
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
                    boolean isRx = rxValue != null &&
                            (rxValue.toString().equals("1") || rxValue.toString().equalsIgnoreCase("true"));
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
        if (!UserSession.getUserRole().equals("admin")) {
            JOptionPane.showMessageDialog(this, "Unauthorized: Only administrators can delete items.");
            return;
        }
        if (selectedItemId == null) return;
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this item?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            itemDAO.removeItem(selectedItemId);
            auditDAO.log("DELETE_ITEM", "items", selectedItemId); // Logging the deletion
            loadTableData();
            clearFields();
        }
    }

    private void clearFields() {
        JTextField[] fields = {nameF, genericF, brandF, barcodeF, dosageF, strengthF, priceF, reorderF};
        for (JTextField field : fields) {
            field.setText("");
        }
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
                cell.setBackground(r % 2 == 0 ? Color.WHITE : new Color(248, 248, 248));
            }
            if (t.getColumnName(c).toLowerCase().contains("prescript")) {
                boolean req = v != null && (v.toString().equals("1") || v.toString().equalsIgnoreCase("true"));
                setText(req ? "RX REQUIRED" : "OTC");
                setForeground(req ? new Color(150, 0, 0) : new Color(0, 100, 0));
                setFont(getFont().deriveFont(Font.BOLD));
            } else {
                setForeground(Color.BLACK);
            }
            return cell;
        }
    }
}