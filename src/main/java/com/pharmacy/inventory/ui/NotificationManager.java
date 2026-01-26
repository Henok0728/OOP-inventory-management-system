package com.pharmacy.inventory.ui;

import com.pharmacy.inventory.dao.WasteDAO;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class NotificationManager {

    public static void checkExpirations(WasteDAO wasteDAO, Component parent, long userId) {
        List<Map<String, Object>> expired = wasteDAO.getExpiredBatches();

        if (!expired.isEmpty()) {
            int choice = JOptionPane.showConfirmDialog(parent,
                    "System found " + expired.size() + " expired batches. Move them to waste records?",
                    "Expiration Alert", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (choice == JOptionPane.YES_OPTION) {
                for (Map<String, Object> item : expired) {
                    // Extracting values from the map returned by getExpiredBatches
                    int batchId = (int) item.get("id");
                    int itemId = (int) item.get("item_id");
                    int qty = (int) item.get("qty");

                    if (qty > 0) {
                        // Step 1: Add to waste table
                        wasteDAO.moveToWaste(batchId, itemId, qty, "expired", (int) userId);

                        // Step 2: Remove from batches table so it disappears from Products/Inventory
                        wasteDAO.removeStockFromBatch(batchId);
                    }
                }
                JOptionPane.showMessageDialog(parent, "Expired items moved to Waste Management.");
            }
        }
    }
}