package com.pharmacy.inventory.service;

import com.fazecast.jSerialComm.SerialPort;
import com.pharmacy.inventory.dao.AuditDAO;
import com.pharmacy.inventory.dao.UserDAO;
import com.pharmacy.inventory.model.User;
import javax.swing.SwingUtilities;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class RFIDService {
    private final UserDAO userDAO;
    private final AuditDAO auditDAO;

    public RFIDService(UserDAO userDAO, AuditDAO auditDAO) {
        this.userDAO = userDAO;
        this.auditDAO = auditDAO;
    }

    public void start() {
        Thread thread = new Thread(() -> {
            SerialPort port = SerialPort.getCommPort("COM7");

            port.setBaudRate(9600);
            port.setNumDataBits(8);
            port.setNumStopBits(SerialPort.ONE_STOP_BIT);
            port.setParity(SerialPort.NO_PARITY);

            // Set timeout to Semi-Blocking to allow the loop to run smoothly
//            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);

            if (port.openPort()) {
                System.out.println("SUCCESS: Listening for RFID on COM7...");

                // We use BufferedReader because it's more reliable than Scanner for Serial
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(port.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String data = line.trim();
                        System.out.println("Serial Input Received: " + data);

                        if (data.contains("RFID_SCAN:")) {
                            String rawTag = data.substring(data.indexOf(":") + 1).trim();
                            processScan(rawTag);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Serial Read Error: " + e.getMessage());
                } finally {
                    port.closePort();
                    System.out.println("Port COM7 closed.");
                }
            } else {
                System.err.println("ERROR: Could not open COM7.");
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void processScan(String tag) {
        String cleanedTag = tag.replace(" ", "").toUpperCase().trim();
//        System.out.println("DEBUG: Looking up tag [" + cleanedTag + "]");

        User user = userDAO.findByRfidTag(cleanedTag);

        if (user != null) {
            boolean isEntering = !user.isInWarehouse();
            try {
                userDAO.updateWarehouseStatus(user.getUserId(), isEntering);

                String action = isEntering ? "WAREHOUSE_ENTRY" : "WAREHOUSE_EXIT";
                String details = user.getName() + (isEntering ? " entered " : " exited ") + "warehouse.";

                auditDAO.logAction(user.getUserId(), action, details);
                System.out.println("DATABASE UPDATED: " + details);

                SwingUtilities.invokeLater(() -> {
                });

            } catch (Exception e) {
                System.err.println("DB ERROR: " + e.getMessage());
            }
        } else {
            System.out.println("WARNING: Unknown tag scanned: " + cleanedTag);
            auditDAO.logAction(0, "UNAUTHORIZED_ACCESS", "Unknown tag: " + cleanedTag);
        }
    }
}