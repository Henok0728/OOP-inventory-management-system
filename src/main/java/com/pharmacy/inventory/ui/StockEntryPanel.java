package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.*;
import com.pharmacy.inventory.model.Batch;
import com.pharmacy.inventory.model.Supplier;
import com.pharmacy.inventory.model.Item;
import com.pharmacy.inventory.util.UserSession;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class StockEntryPanel extends JPanel {

    private final BatchDAO batchDAO;
    private final ItemDAO itemDAO;
    private final SupplierDAO supplierDAO;
    private final PurchaseDAO purchaseDAO;
    private final AuditDAO auditDAO;

    private JComboBox<Object[]> itemCombo;
    private JComboBox<Supplier> supplierCombo;
    private JComboBox<String> pendingOrderCombo;
    private JTextField batchNumF = new JTextField();
    private JTextField qtyF = new JTextField();
    private JTextField purchasePriceF = new JTextField();
    private JTextField sellingPriceF = new JTextField();
    private JTextField mfgDateF = new JTextField("YYYY-MM-DD");
    private JTextField expDateF = new JTextField("YYYY-MM-DD");
    private JTextField storageF = new JTextField();

    public StockEntryPanel(BatchDAO batchDAO, ItemDAO itemDAO, SupplierDAO supplierDAO,
                           PurchaseDAO purchaseDAO, AuditDAO auditDAO) {
        this.batchDAO = batchDAO;
        this.itemDAO = itemDAO;
        this.supplierDAO = supplierDAO;
        this.purchaseDAO = purchaseDAO;
        this.auditDAO = auditDAO;

        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setBackground(new Color(240, 242, 245));

        initializeUI();
    }

    private void initializeUI() {
        JLabel header = new JLabel("📦 GRN Verification & Stock Receipt");
        header.setFont(new Font("SansSerif", Font.BOLD, 22));
        add(header, BorderLayout.NORTH);

        JPanel formContainer = new JPanel(new GridBagLayout());
        formContainer.setBackground(Color.WHITE);
        formContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(25, 25, 25, 25)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formContainer.add(new JLabel("1. Select PO Number:"), gbc);
        gbc.gridx = 1;
        pendingOrderCombo = new JComboBox<>();
        pendingOrderCombo.addActionListener(e -> onOrderSelected());
        formContainer.add(pendingOrderCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formContainer.add(new JLabel("2. Select Item from PO:"), gbc);
        gbc.gridx = 1;
        itemCombo = new JComboBox<>();
        itemCombo.addActionListener(e -> onItemFromPOSelected());
        formContainer.add(itemCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formContainer.add(new JLabel("Supplier:"), gbc);
        gbc.gridx = 1;
        supplierCombo = new JComboBox<>();
        supplierCombo.setEnabled(false); // Locked for security
        formContainer.add(supplierCombo, gbc);

        addFormField(formContainer, gbc, 3, "Batch Number:", batchNumF);
        addFormField(formContainer, gbc, 4, "Qty to Receive:", qtyF);
        qtyF.setEditable(false); // Locked to PO quantity
        addFormField(formContainer, gbc, 5, "Purchase Price (Locked):", purchasePriceF);
        purchasePriceF.setEditable(false); // Locked to PO price

        addFormField(formContainer, gbc, 6, "Set Selling Price:", sellingPriceF);
        addFormField(formContainer, gbc, 7, "Mfg Date:", mfgDateF);
        addFormField(formContainer, gbc, 8, "Expiry Date:", expDateF);
        addFormField(formContainer, gbc, 9, "Storage Area:", storageF);

        JButton saveBtn = new JButton("Verify & Add to Inventory");
        saveBtn.setBackground(new Color(40, 167, 69));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setPreferredSize(new Dimension(220, 40));
        saveBtn.addActionListener(e -> handleSave());

        gbc.gridx = 0; gbc.gridy = 10; gbc.gridwidth = 2;
        formContainer.add(saveBtn, gbc);

        add(new JScrollPane(formContainer), BorderLayout.CENTER);
        refreshData();
    }

    private void addFormField(JPanel p, GridBagConstraints gbc, int row, String lab, JTextField f) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        p.add(new JLabel(lab), gbc);
        gbc.gridx = 1;
        p.add(f, gbc);
    }

    private void onOrderSelected() {
        Object selected = pendingOrderCombo.getSelectedItem();
        itemCombo.removeAllItems();

        if (selected != null && !selected.toString().equals("Select PO...")) {
            long pId = Long.parseLong(selected.toString());

            // 1. Lock Supplier
            int sId = purchaseDAO.getSupplierIdByPurchase(pId);
            for (int i = 0; i < supplierCombo.getItemCount(); i++) {
                if (supplierCombo.getItemAt(i).getSupplierId() == sId) {
                    supplierCombo.setSelectedIndex(i);
                    break;
                }
            }

            // 2. Load only items belonging to this PO
            List<Object[]> pendingItems = purchaseDAO.getPendingItemsInPO(pId);
            for (Object[] row : pendingItems) {
                // row: {itemId, name, qty, price}
                itemCombo.addItem(row);
            }
            itemCombo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, isSelected, index, isSelected, cellHasFocus);
                    if (value instanceof Object[]) {
                        setText(((Object[]) value)[1].toString());
                    }
                    return this;
                }
            });
        }
    }

    private void onItemFromPOSelected() {
        Object selected = itemCombo.getSelectedItem();
        if (selected instanceof Object[]) {
            Object[] data = (Object[]) selected;
            qtyF.setText(data[2].toString());
            purchasePriceF.setText(data[3].toString());
        } else {
            qtyF.setText("");
            purchasePriceF.setText("");
        }
    }

    private void handleSave() {
        try {
            Object[] itemData = (Object[]) itemCombo.getSelectedItem();
            String poIdStr = (String) pendingOrderCombo.getSelectedItem();

            if (itemData == null || poIdStr.equals("Select PO...")) throw new Exception("Please select a PO and an Item.");

            int itemId = (int) itemData[0];
            int qty = Integer.parseInt(qtyF.getText());
            double pPrice = Double.parseDouble(purchasePriceF.getText());

            // Create Batch
            Batch b = new Batch();
            b.setItemId(itemId);
            b.setBatchNumber(batchNumF.getText());
            b.setQuantityReceived(qty);
            b.setQuantityRemaining(qty);
            b.setPurchasePrice(pPrice);
            b.setSellingPrice(Double.parseDouble(sellingPriceF.getText()));
            b.setManufacturedDate(mfgDateF.getText());
            b.setExpirationDate(expDateF.getText());
            b.setStorageLocation(storageF.getText());
            b.setStatus("active");
            b.setReceivedDate(new java.sql.Date(System.currentTimeMillis()).toString());

            if (batchDAO.addBatch(b)) {
                // Update PO status and check for closure
                long poId = Long.parseLong(poIdStr);
                purchaseDAO.fulfillItemAndCheckClosure(poId, itemId, (qty * pPrice));

                auditDAO.log("GRN_ITEM_RECEIVED", "batches", itemId);
                JOptionPane.showMessageDialog(this, "Item received and added to inventory!");
                clearForm();
                refreshData();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void refreshData() {
        supplierCombo.removeAllItems();
        supplierDAO.getAllSuppliers().forEach(supplierCombo::addItem);

        pendingOrderCombo.removeAllItems();
        pendingOrderCombo.addItem("Select PO...");

        // Show all approved orders that are still pending
        if (UserSession.getCurrentUser() != null) {
            purchaseDAO.getMyApprovedOrders(UserSession.getCurrentUser().getUserId())
                    .forEach(id -> pendingOrderCombo.addItem(String.valueOf(id)));
        }
    }

    private void clearForm() {
        batchNumF.setText(""); sellingPriceF.setText("");
        mfgDateF.setText("YYYY-MM-DD"); expDateF.setText("YYYY-MM-DD");
        storageF.setText("");
    }
}