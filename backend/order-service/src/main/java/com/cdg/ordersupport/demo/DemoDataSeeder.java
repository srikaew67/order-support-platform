package com.cdg.ordersupport.demo;

import com.cdg.ordersupport.product.Product;
import com.cdg.ordersupport.product.ProductRepository;
import com.cdg.ordersupport.user.Role;
import com.cdg.ordersupport.user.User;
import com.cdg.ordersupport.user.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@ConditionalOnProperty(prefix = "demo.data", name = "enabled", havingValue = "true")
class DemoDataSeeder {
    @Bean
    ApplicationRunner seedDemoData(
            UserRepository users,
            ProductRepository products,
            PasswordEncoder passwords,
            @Value("${demo.admin.email:admin@order-support.local}") String adminEmail,
            @Value("${demo.admin.password:}") String adminPassword) {
        return ignored -> {
            if (adminPassword.isBlank()) {
                throw new IllegalStateException("demo.admin.password must be set when demo.data.enabled=true");
            }
            if (users.findByEmail(adminEmail).isEmpty()) {
                users.save(new User(adminEmail, passwords.encode(adminPassword), "Demo Administrator", Role.ADMIN));
            }
            demoProducts().forEach(product -> {
                if (!products.existsBySku(product.getSku())) {
                    products.save(product);
                }
            });
        };
    }

    private List<Product> demoProducts() {
        return List.of(
                new Product("DEMO-HEADSET", "Wireless Headset", "Noise-cancelling headset for support calls.", new BigDecimal("79.90"), 25),
                new Product("DEMO-KEYBOARD", "Mechanical Keyboard", "Compact mechanical keyboard with Thai layout.", new BigDecimal("64.90"), 18),
                new Product("DEMO-LAPTOP-STAND", "Aluminium Laptop Stand", "Adjustable stand for a comfortable desk setup.", new BigDecimal("39.90"), 40),
                new Product("DEMO-MOUSE", "Ergonomic Mouse", "Wireless ergonomic mouse for daily work.", new BigDecimal("34.90"), 30),
                new Product("DEMO-WEBCAM", "Full HD Webcam", "1080p webcam with built-in microphone.", new BigDecimal("54.90"), 15),
                new Product("DEMO-USB-C-HUB", "USB-C Hub", "Seven-port USB-C hub with HDMI output.", new BigDecimal("49.90"), 22));
    }
}
