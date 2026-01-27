package com.pharmacy.inventory.server;

import com.pharmacy.inventory.dao.ItemDAO;
import com.pharmacy.inventory.model.Item;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

@Component
public class PharmacyServer {

    @Autowired
    private ItemDAO itemDAO;

    @PostConstruct
    public void startServer() {
        new Thread(() -> {
            try {
                // Listening on all network interfaces on port 8080
                HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

                // Endpoint for the phone scanner
                server.createContext("/api/scan", new ScanHandler());

                // Endpoint for general item search
                server.createContext("/api/search", new SearchHandler());

                server.setExecutor(null);
                server.start();
                System.out.println("🚀 Pharmacy Server is LIVE at http://192.168.10.60:8080");
            } catch (IOException e) {
                System.err.println("❌ Critical Error: Could not start server: " + e.getMessage());
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
                // Extract barcode from ?code=XXXXXXXX
                if (query != null && query.contains("code=")) {
                    String barcode = query.split("code=")[1].trim();
                    System.out.println("📥 Phone Scanned Barcode: " + barcode);

                    Item item = itemDAO.findItemByBarcode(barcode);

                    if (item != null) {
                        response = String.format("{\"status\":\"found\", \"name\":\"%s\", \"price\":%.2f}",
                                item.getName(), item.getRetailPrice());

                        // --- UI INTEGRATION ---
                        // This updates your Desktop UI without crashing it
                        SwingUtilities.invokeLater(() -> {
                            System.out.println("📢 Action: Displaying " + item.getName() + " on Desktop UI");
                            // You can call inventory.showProduct(item) here
                        });
                    } else {
                        response = "{\"status\":\"error\", \"message\":\"Medicine not found in database\"}";
                    }
                } else {
                    response = "{\"status\":\"error\", \"message\":\"No barcode provided\"}";
                    statusCode = 400;
                }
            } catch (Exception e) {
                response = "{\"status\":\"error\", \"message\":\"Server Error\"}";
                statusCode = 500;
            }

            sendJsonResponse(exchange, response, statusCode);
        }
    }

    private class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Logic for manual search from phone
            String query = exchange.getRequestURI().getQuery().split("=")[1];
            Item item = itemDAO.findItemByBarcode(query);
            String response = (item != null) ? "{\"name\":\"" + item.getName() + "\"}" : "{\"error\":\"none\"}";
            sendJsonResponse(exchange, response, 200);
        }
    }

    private void sendJsonResponse(HttpExchange exchange, String response, int code) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}