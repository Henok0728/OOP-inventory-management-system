package com.pharmacy.inventory.server;

import com.pharmacy.inventory.dao.ItemDAO;
import com.pharmacy.inventory.ui.Inventory;
import com.pharmacy.inventory.ui.SalesPanel;
import com.pharmacy.inventory.model.Item;
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

    /**
     * This is called from the Inventory class during GUI preparation
     * to link the server to the active Sales UI.
     */
    public void setSalesPanel(SalesPanel panel) {
        PharmacyServer.salesPanel = panel;
    }

    @PostConstruct
    public void startServer() {
        new Thread(() -> {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

<<<<<<< Updated upstream
                server.createContext("/api/scan", new ScanHandler());

=======
                // Endpoint for Binary Eye (e.g., /api/scan?code=123456)
                server.createContext("/api/scan", new ScanHandler());

                // Endpoint for general item search
>>>>>>> Stashed changes
                server.createContext("/api/search", new SearchHandler());

                server.setExecutor(null);
                server.start();
<<<<<<< Updated upstream
                System.out.println("Pharmacy Server is LIVE at http://192.168.10.60:8080");
=======
                System.out.println("🚀 Pharmacy Server LIVE at http://192.168.10.60:8080");
                System.out.println("📱 Binary Eye URL: http://192.168.10.60:8080/api/scan?code={RESULT}");
>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
            try {
                // Your phone sends: ?content=0076950450479&raw=...
                if (query != null && query.contains("content=")) {
                    // Extracting the 'content' value from the query string
                    String barcode = query.split("content=")[1].split("&")[0];
                    System.out.println("Phone Scanned Barcode: " + barcode);
=======
            // 1. Validate the query string for Binary Eye's 'code=' parameter
            if (query != null && query.contains("code=")) {
                try {
                    // Extracting the barcode value
                    String barcode = query.split("code=")[1].split("&")[0];
                    System.out.println("📥 Binary Eye Scanned: " + barcode);
>>>>>>> Stashed changes

                    if (salesPanel != null) {
                        // 2. Safely update UI from the background server thread
                        SwingUtilities.invokeLater(() -> {
                            // Switch the CardLayout to the Sales page
                            Inventory.showPage("Sales");
                            // Push barcode to search bar and trigger processSearch()
                            salesPanel.remoteBarcodeScanned(barcode);
                        });

                        response = "{\"status\":\"success\", \"barcode\":\"" + barcode + "\"}";
                    } else {
                        response = "{\"status\":\"error\", \"message\":\"Sales Panel not initialized\"}";
                        statusCode = 500;
                    }
                } catch (Exception e) {
                    response = "{\"status\":\"error\", \"message\":\"Failed to parse barcode\"}";
                    statusCode = 400;
                }
            } else {
                response = "{\"status\":\"error\", \"message\":\"Missing 'code' parameter\"}";
                statusCode = 400;
            }

            sendJsonResponse(exchange, response, statusCode);
        }
    }

    private class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String queryParam = exchange.getRequestURI().getQuery();
            if (queryParam == null || !queryParam.contains("=")) {
                sendJsonResponse(exchange, "{\"error\":\"invalid query\"}", 400);
                return;
            }

            String query = queryParam.split("=")[1];
            Item item = itemDAO.findItemByBarcode(query);
            String response = (item != null) ? "{\"name\":\"" + item.getName() + "\"}" : "{\"error\":\"none\"}";
            sendJsonResponse(exchange, response, 200);
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