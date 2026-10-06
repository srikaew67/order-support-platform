package com.cdg.ordersupport.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.cdg.ordersupport.product.ProductRepository;
import com.cdg.ordersupport.user.Role;
import com.cdg.ordersupport.user.UserRepository;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:demo_data_seeder;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "demo.data.enabled=true",
        "demo.admin.email=admin@order-support.local",
        "demo.admin.password=Admin-demo-123"
})
@ActiveProfiles("test")
class DemoDataSeederIT {
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired PasswordEncoder passwords;
    @Autowired @Qualifier("seedDemoData") ApplicationRunner seeder;

    @Test
    void createsAdminAndCatalogWhenDemoDataIsEnabled() {
        assertThat(users.findByEmail("admin@order-support.local")).hasValueSatisfying(admin -> {
            assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
            assertThat(passwords.matches("Admin-demo-123", admin.getPasswordHash())).isTrue();
        });
        assertThat(products.findAll().stream().map(product -> product.getSku()).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(
                        "DEMO-HEADSET", "DEMO-KEYBOARD", "DEMO-LAPTOP-STAND",
                        "DEMO-MOUSE", "DEMO-WEBCAM", "DEMO-USB-C-HUB");
    }

    @Test
    void doesNotDuplicateAdminOrProductsWhenRunAgain() throws Exception {
        seeder.run(null);

        assertThat(users.findAll()).filteredOn(user -> user.getEmail().equals("admin@order-support.local"))
                .hasSize(1);
        assertThat(products.findAll()).filteredOn(product -> product.getSku().startsWith("DEMO-"))
                .hasSize(6);
    }
}
