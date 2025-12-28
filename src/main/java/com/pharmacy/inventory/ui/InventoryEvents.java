package com.pharmacy.inventory.ui;

import java.awt.event.*;
import java.awt.*;

public class InventoryEvents implements ActionListener {
    private final Inventory mainUI;

    public InventoryEvents(Inventory mainUI) {
        this.mainUI = mainUI;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        if (command.equals("Products")) {
            // 1. Clear the panel
            mainUI.rightRight.removeAll();

            // 2. Instead of using the fake 'DisplaysTable',
            // we call the loadTableData() method we added to your Inventory class
            mainUI.loadTableData();

            // 3. Refresh the UI
            mainUI.rightRight.revalidate();
            mainUI.rightRight.repaint();
        }

        // You can add more logic here for "Add", "Edit", or "Delete"
        else if (command.equals("Add")) {
            System.out.println("Add button clicked! We should pull data from fields here.");
        }

        mainUI.products.revalidate();
        mainUI.products.repaint();
    }
}