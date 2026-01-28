package com.pharmacy.inventory.server;

import com.pharmacy.inventory.dao.ItemDAO;
import com.pharmacy.inventory.ui.SalesPanel;
import com.pharmacy.inventory.model.Item;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

@Component
public class PharmacyServer {

    @Autowired
    private ItemDAO itemDAO;

    // Static reference to the UI to allow the server to "push" barcodes to the screen
    private static SalesPanel salesPanel;

    public void setSalesPanel(SalesPanel panel) {
        PharmacyServer.salesPanel = panel;
    }

    @PostConstruct
    public void startServer() {
        new Thread(() -> {
            try {
                // Listening on all network interfaces on port 8080
                HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

                // Endpoint for the phone scanner (Barcode to Cart)
                server.createContext("/api/scan", new ScanHandler());

                // Endpoint for general item search (Information only)
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
                // Your phone sends: ?content=0076950450479&raw=...
                if (query != null && query.contains("content=")) {
                    // Extracting the 'content' value from the query string
                    String barcode = query.split("content=")[1].split("&")[0];
                    System.out.println("📥 Phone Scanned Barcode: " + barcode);

                    // 1. Verify if the item exists in DB
                    Item item = itemDAO.findItemByBarcode(barcode);

                    if (item != null) {
                        response = String.format("{\"status\":\"success\", \"name\":\"%s\"}", item.getName());

                        // 2. Push to SalesPanel if the UI is currently open
                        if (salesPanel != null) {
                            salesPanel.remoteBarcodeScanned(barcode);
                        }
                    } else {
                        response = "{\"status\":\"error\", \"message\":\"Product not in database\"}";
                    }
                } else {
                    response = "{\"status\":\"error\", \"message\":\"Invalid scan data\"}";
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
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        // Ensure CORS is allowed so the phone can connect without issues
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}