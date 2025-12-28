package com.pharmacy.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.pharmacy.inventory.ui.Inventory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class PharmacyManagementSystemApplication {

    public static void main(String[] args) {

        // This starts Spring, handles the Proxy, connects to DB,
        // and creates the Inventory bean automatically.
        SpringApplicationBuilder builder = new SpringApplicationBuilder(PharmacyManagementSystemApplication.class);
        builder.headless(false); // Necessary for Swing/AWT to work in Spring Boot
        ConfigurableApplicationContext context = builder.run(args);
    }

}
