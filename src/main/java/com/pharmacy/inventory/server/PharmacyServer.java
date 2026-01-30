package com.pharmacy.inventory.server;

import com.pharmacy.inventory.dao.ItemDAO;
import com.pharmacy.inventory.ui.Inventory;
import com.pharmacy.inventory.ui.SalesPanel;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

import javax.swing.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

@Component
public class PharmacyServer {

    @Autowired
    private ItemDAO itemDAO;

    private static SalesPanel salesPanel;

    public void setSalesPanel(SalesPanel panel) {
        PharmacyServer.salesPanel = panel;
    }

    @PostConstruct
    public void startServer() {
        new Thread(() -> {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
                server.createContext("/api/scan", new ScanHandler());
                server.setExecutor(null);
                server.start();
                System.out.println("Check 01");
            } catch (IOException e) {
                System.err.println("Critical Error: Could not start server: " + e.getMessage());
            }
        }).start();
    }

    private class ScanHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String response;
            int statusCode = 200;

            try {
                if (query != null && query.contains("code=")) {
                    String barcode = query.split("code=")[1].split("&")[0];
                    if (salesPanel != null) {
                        SwingUtilities.invokeLater(() -> {
                            Inventory.showPage("Sales");
                            salesPanel.remoteBarcodeScanned(barcode);
                        });
                        response = "{\"status\":\"success\", \"barcode\":\"" + barcode + "\"}";
                    } else {
                        response = "{\"status\":\"error\", \"message\":\"Sales Panel not linked\"}";
                        statusCode = 500;
                    }
                } else {
                    response = "{\"status\":\"error\", \"message\":\"Missing 'code' parameter\"}";
                    statusCode = 400;
                }
            } catch (Exception e) {
                e.printStackTrace();
                response = "{\"status\":\"error\", \"message\":\"Server Error\"}";
                statusCode = 500;
            }

            sendJsonResponse(exchange, response, statusCode);
        }
    }

    private void sendJsonResponse(HttpExchange exchange, String response, int code) throws IOException {
        byte[] bytes = response.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}