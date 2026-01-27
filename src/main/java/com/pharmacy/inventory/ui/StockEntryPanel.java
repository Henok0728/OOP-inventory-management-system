package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.BatchDAO;
import com.pharmacy.inventory.dao.ItemDAO;
import com.pharmacy.inventory.dao.SupplierDAO;
import com.pharmacy.inventory.model.Batch;
import com.pharmacy.inventory.model.Supplier;
import com.pharmacy.inventory.model.Item;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

public class StockEntryPanel extends JPanel {

    private final BatchDAO batchDAO;
    private final ItemDAO itemDAO;
    private final SupplierDAO supplierDAO;

    // Components
    private JComboBox<Item> itemCombo;
    private JComboBox<Supplier> supplierCombo;
    private JTextField batchNumF = new JTextField();
    private JTextField qtyF = new JTextField();
    private JTextField purchasePriceF = new JTextField();
    private JTextField sellingPriceF = new JTextField();
    private JTextField mfgDateF = new JTextField("YYYY-MM-DD");
    private JTextField expDateF = new JTextField("YYYY-MM-DD");
    private JTextField storageF = new JTextField();

    public StockEntryPanel(BatchDAO batchDAO, ItemDAO itemDAO, SupplierDAO supplierDAO) {
        this.batchDAO = batchDAO;
        this.itemDAO = itemDAO;
        this.supplierDAO = supplierDAO;

        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setBackground(new Color(240, 242, 245));

        initializeUI();
    }

    private void initializeUI() {
        // --- HEADER ---
        JLabel header = new JLabel("📦 New Stock Receipt (Batch Entry)");
        header.setFont(new Font("SansSerif", Font.BOLD, 22));
        add(header, BorderLayout.NORTH);

        // --- FORM PANEL ---
        JPanel formContainer = new JPanel(new GridBagLayout());
        formContainer.setBackground(Color.WHITE);
        formContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(25, 25, 25, 25)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Select Item
        gbc.gridx = 0; gbc.gridy = 0;
        formContainer.add(new JLabel("Select Medicine:"), gbc);
        gbc.gridx = 1;
        itemCombo = new JComboBox<>();
        formContainer.add(itemCombo, gbc);

        // Row 1: Select Supplier
        gbc.gridx = 0; gbc.gridy = 1;
        formContainer.add(new JLabel("Supplier:"), gbc);
        gbc.gridx = 1;
        supplierCombo = new JComboBox<>();
        formContainer.add(supplierCombo, gbc);

        // Row 2: Batch Number & Storage
        gbc.gridx = 0; gbc.gridy = 2;
        formContainer.add(new JLabel("Batch Number:"), gbc);
        gbc.gridx = 1;
        formContainer.add(batchNumF, gbc);

        // Row 3: Quantity & Storage
        gbc.gridx = 0; gbc.gridy = 3;
        formContainer.add(new JLabel("Quantity Received:"), gbc);
        gbc.gridx = 1;
        formContainer.add(qtyF, gbc);

        // Row 4: Pricing
        gbc.gridx = 0; gbc.gridy = 4;
        formContainer.add(new JLabel("Purchase Price (Unit):"), gbc);
        gbc.gridx = 1;
        formContainer.add(purchasePriceF, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        formContainer.add(new JLabel("Selling Price (Unit):"), gbc);
        gbc.gridx = 1;
        formContainer.add(sellingPriceF, gbc);

        // Row 6: Dates
        gbc.gridx = 0; gbc.gridy = 6;
        formContainer.add(new JLabel("Mfg Date:"), gbc);
        gbc.gridx = 1;
        formContainer.add(mfgDateF, gbc);

        gbc.gridx = 0; gbc.gridy = 7;
        formContainer.add(new JLabel("Expiry Date:"), gbc);
        gbc.gridx = 1;
        formContainer.add(expDateF, gbc);

        // Row 8: Storage Location
        gbc.gridx = 0; gbc.gridy = 8;
        formContainer.add(new JLabel("Storage Shelf/Area:"), gbc);
        gbc.gridx = 1;
        formContainer.add(storageF, gbc);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        JButton saveBtn = new JButton("Confirm Stock Entry");
        saveBtn.setBackground(new Color(40, 167, 69));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setPreferredSize(new Dimension(180, 40));
        saveBtn.addActionListener(e -> handleSave());

        btnPanel.add(saveBtn);

        gbc.gridx = 0; gbc.gridy = 9; gbc.gridwidth = 2;
        formContainer.add(btnPanel, gbc);

        add(formContainer, BorderLayout.CENTER);

        refreshData();
    }

    private void handleSave() {
        try {
            Item selectedItem = (Item) itemCombo.getSelectedItem();
            Supplier selectedSupplier = (Supplier) supplierCombo.getSelectedItem();

            if (selectedItem == null) throw new Exception("Please select a medicine.");

            Batch b = new Batch();
            b.setItemId(selectedItem.getItem_id());
            b.setBatchNumber(batchNumF.getText());
            b.setQuantityReceived(Integer.parseInt(qtyF.getText()));
            b.setPurchasePrice(Double.parseDouble(purchasePriceF.getText()));
            b.setSellingPrice(Double.parseDouble(sellingPriceF.getText()));
            b.setManufacturedDate(mfgDateF.getText());
            b.setExpirationDate(expDateF.getText());
            b.setStorageLocation(storageF.getText());
            b.setStatus("active");

            if (batchDAO.addBatch(b)) {
                JOptionPane.showMessageDialog(this, "Inventory updated successfully!");
                clearForm();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    public void refreshData() {

        itemCombo.removeAllItems();
        supplierCombo.removeAllItems();

        List<Item> items = itemDAO.getAllItemsList();
        for (Item i : items) itemCombo.addItem(i);

        List<Supplier> suppliers = supplierDAO.getAllSuppliers();
        for (Supplier s : suppliers) supplierCombo.addItem(s);
    }

    private void clearForm() {
        batchNumF.setText("");
        qtyF.setText("");
        purchasePriceF.setText("");
        sellingPriceF.setText("");
        mfgDateF.setText("YYYY-MM-DD");
        expDateF.setText("YYYY-MM-DD");
        storageF.setText("");
    }
}