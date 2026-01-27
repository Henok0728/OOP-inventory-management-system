package com.pharmacy.inventory.service;

import com.fazecast.jSerialComm.SerialPort;
import com.pharmacy.inventory.dao.AuditDAO;
import com.pharmacy.inventory.dao.UserDAO;
import com.pharmacy.inventory.model.User;
import javax.swing.SwingUtilities;
import java.util.Scanner;

public class RFIDService {
    private final UserDAO userDAO;
    private final AuditDAO auditDAO;

    public RFIDService(UserDAO userDAO, AuditDAO auditDAO) {
        this.userDAO = userDAO;
        this.auditDAO = auditDAO;
    }

    public void start() {
        Thread thread = new Thread(() -> {
            // Ensure this matches your Arduino port
            SerialPort port = SerialPort.getCommPort("COM7");
            port.setBaudRate(9600);

            // Inside your start() method thread
            if (port.openPort()) {
                System.out.println("RFID Scanner active on " + port.getSystemPortName());

                // Set a timeout so the read doesn't block forever
                port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);

                try (Scanner scanner = new Scanner(port.getInputStream())) {
                    // Use a delimiter that matches what Arduino sends (usually \r\n)
                    while (scanner.hasNext()) {
                        String line = scanner.next(); // Read the next token

                        // Log exactly what Java sees
                        System.out.println("Java received: " + line);

                        if (line.contains("RFID_SCAN:")) {
                            String rawTag = line.substring(line.indexOf(":") + 1).trim();
                            processScan(rawTag);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Serial Read Error: " + e.getMessage());
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void processScan(String tag) {
        System.out.println("DEBUG: processScan called with tag: " + tag);
        // 1. Clean the tag string (Remove spaces, make Uppercase)
        // This ensures '83 a2 0b' matches '83A20B' in the database
        String cleanedTag = tag.replace(" ", "").toUpperCase().trim();
        System.out.println("--- New Scan Detected ---");
        System.out.println("Raw Tag: " + tag);
        System.out.println("Cleaned Tag: " + cleanedTag);

        // 2. Search for the user in the database
        User user = userDAO.findByRfidTag(cleanedTag);

        if (user != null) {
            System.out.println("User Identified: " + user.getName());

            // 3. Logic: Toggle Status
            boolean isEntering = !user.isInWarehouse();

            try {
                // Update the user's current location in DB
                userDAO.updateWarehouseStatus(user.getUserId(), isEntering);

                // 4. Log the action to audit_logs
                String action = isEntering ? "WAREHOUSE_ENTRY" : "WAREHOUSE_EXIT";
                String details = user.getName() + (isEntering ? " entered " : " exited ") + "the warehouse.";

                auditDAO.logAction(user.getUserId(), action, details);
                System.out.println("Database Updated: " + action);

                // 5. Alert the UI
                SwingUtilities.invokeLater(() -> {
                    // This will print to your IDE console, but you could also
                    // trigger a JOptionPane or UI label update here
                    System.out.println("UI NOTIFICATION: " + details);
                });

            } catch (Exception e) {
                System.err.println("Database Error during RFID processing: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Log unauthorized or unknown tag attempts
            System.out.println("WARNING: Unknown RFID Tag scanned: " + cleanedTag);
            auditDAO.logAction(0, "UNAUTHORIZED_ACCESS", "Unknown tag: " + cleanedTag);
        }
    }
}