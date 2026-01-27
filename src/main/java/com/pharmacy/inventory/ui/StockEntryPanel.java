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

    private JComboBox<Item> itemCombo;
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
        JLabel header = new JLabel("📦 Stock Receipt & Reconciliation");
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
        formContainer.add(new JLabel("Fulfill Approved Order:"), gbc);
        gbc.gridx = 1;
        pendingOrderCombo = new JComboBox<>();
        pendingOrderCombo.addActionListener(e -> onOrderSelected());
        formContainer.add(pendingOrderCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formContainer.add(new JLabel("Select Medicine:"), gbc);
        gbc.gridx = 1;
        itemCombo = new JComboBox<>();
        formContainer.add(itemCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formContainer.add(new JLabel("Supplier:"), gbc);
        gbc.gridx = 1;
        supplierCombo = new JComboBox<>();
        formContainer.add(supplierCombo, gbc);

        addFormField(formContainer, gbc, 3, "Batch Number:", batchNumF);
        addFormField(formContainer, gbc, 4, "Quantity Received:", qtyF);
        addFormField(formContainer, gbc, 5, "Purchase Price (Unit):", purchasePriceF);
        addFormField(formContainer, gbc, 6, "Selling Price (Unit):", sellingPriceF);
        addFormField(formContainer, gbc, 7, "Mfg Date:", mfgDateF);
        addFormField(formContainer, gbc, 8, "Expiry Date:", expDateF);
        addFormField(formContainer, gbc, 9, "Storage Area:", storageF);

        JButton saveBtn = new JButton("Generate GRN & Save Stock");
        saveBtn.setToolTipText("Reconciles physical delivery with the Purchase Order");
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
        if (selected != null && !selected.toString().equals("None (New Purchase)")) {
            long pId = Long.parseLong(selected.toString());
            int sId = purchaseDAO.getSupplierIdByPurchase(pId);
            for (int i = 0; i < supplierCombo.getItemCount(); i++) {
                if (supplierCombo.getItemAt(i).getSupplierId() == sId) {
                    supplierCombo.setSelectedIndex(i);
                    supplierCombo.setEnabled(false);
                    break;
                }
            }
        } else {
            supplierCombo.setEnabled(true);
        }
    }

    private void handleSave() {
        try {
            Item item = (Item) itemCombo.getSelectedItem();
            Supplier supplier = (Supplier) supplierCombo.getSelectedItem();
            String poIdStr = (String) pendingOrderCombo.getSelectedItem();

            if (item == null || supplier == null) throw new Exception("Medicine and Supplier required.");


            int qty = Integer.parseInt(qtyF.getText());
            double price = Double.parseDouble(purchasePriceF.getText());
            double actualTotal = qty * price;

            // Variance Check
            if (poIdStr != null && !poIdStr.equals("None (New Purchase)")) {
                long poId = Long.parseLong(poIdStr);
                double approvedAmt = purchaseDAO.getApprovedAmount(poId);

                if (actualTotal > (approvedAmt * 1.15)) { // 15% Variance threshold
                    int res = JOptionPane.showConfirmDialog(this,
                            "Actual total is significantly higher than approved. Proceed?",
                            "Price Variance", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (res != JOptionPane.YES_OPTION) return;
                }
            }

            // Create Batch
            Batch b = new Batch();
            b.setItemId(item.getItem_id());
            b.setBatchNumber(batchNumF.getText());
            b.setQuantityReceived(qty);
            b.setPurchasePrice(price);
            b.setSellingPrice(Double.parseDouble(sellingPriceF.getText()));
            b.setManufacturedDate(mfgDateF.getText());
            b.setExpirationDate(expDateF.getText());
            b.setStorageLocation(storageF.getText());
            b.setStatus("active");

            if (batchDAO.addBatch(b)) {
                supplierDAO.linkItemToSupplier(item.getItem_id(), supplier.getSupplierId());

                if (poIdStr != null && !poIdStr.equals("None (New Purchase)")) {
                    // Formal Close
                    purchaseDAO.reconcileAndClose(Long.parseLong(poIdStr), actualTotal);
                } else {

                    purchaseDAO.createPurchaseOrder(supplier.getSupplierId(), actualTotal,
                            UserSession.getCurrentUser().getUserId(), "admin");

                }

                auditDAO.log("STOCK_RECEIPT", "batches", item.getItem_id());
                JOptionPane.showMessageDialog(this, "Inventory updated and PO reconciled!");
                clearForm();
                refreshData();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    public void refreshData() {
        itemCombo.removeAllItems();
        supplierCombo.removeAllItems();
        pendingOrderCombo.removeAllItems();

        itemDAO.getAllItemsList().forEach(itemCombo::addItem);
        supplierDAO.getAllSuppliers().forEach(supplierCombo::addItem);
        pendingOrderCombo.addItem("None (New Purchase)");

        if (UserSession.getCurrentUser() != null) {
            purchaseDAO.getMyApprovedOrders(UserSession.getCurrentUser().getUserId())
                    .forEach(id -> pendingOrderCombo.addItem(String.valueOf(id)));
        }
    }

    private void clearForm() {
        batchNumF.setText(""); qtyF.setText(""); purchasePriceF.setText("");
        sellingPriceF.setText(""); mfgDateF.setText("YYYY-MM-DD");
        expDateF.setText("YYYY-MM-DD"); storageF.setText("");
        pendingOrderCombo.setSelectedIndex(0); supplierCombo.setEnabled(true);
    }
}