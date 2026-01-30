package com.pharmacy.inventory.service;

import com.fazecast.jSerialComm.SerialPort;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class EnvironmentService {
    // Static variables for the UI to access
    private static double warehouseTemp = 0.0;
    private static double warehouseHum = 0.0;
    private static double outsideTemp = 0.0;

    private final String portName;

    public EnvironmentService(String portName) {
        this.portName = portName;
    }

    public void start() {
        Thread thread = new Thread(() -> {
            SerialPort port = SerialPort.getCommPort(portName);
            port.setBaudRate(9600);
            // Using Semi-Blocking to keep the thread alive while waiting for lines
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);

            if (port.openPort()) {
                System.out.println("✅ Environment Service: Connected to " + portName);

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(port.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String data = line.trim();
                        if (data.isEmpty()) continue;

                        parseArduinoInput(data);
                    }
                } catch (Exception e) {
                    System.err.println("❌ Serial Read Error: " + e.getMessage());
                } finally {
                    port.closePort();
                }
            } else {
                System.err.println("❌ Error: Could not open " + portName + ". Is it in use?");
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void parseArduinoInput(String data) {
        try {
            if (data.startsWith("TEMP:")) {
                warehouseTemp = Double.parseDouble(data.substring(5));
            } else if (data.startsWith("HUM:")) {
                warehouseHum = Double.parseDouble(data.substring(4));
            } else {
                // If it's a raw number (from your outTemp class)
                outsideTemp = Double.parseDouble(data);
            }
        } catch (NumberFormatException e) {
            // Ignore lines that aren't valid numbers
        }
    }

    // Getters for the HomePanel
    public static double getWarehouseTemp() { return warehouseTemp; }
    public static double getWarehouseHum() { return warehouseHum; }
    public static double getOutsideTemp() { return outsideTemp; }
}