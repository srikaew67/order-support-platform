package com.cdg.ordersupport.demo;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.cdg.ordersupport.product.ProductRepository;
import com.cdg.ordersupport.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

class DemoDataSeederTest {
    @Test
    void rejectsMissingAdminPasswordWhenDemoDataIsEnabled() {
        ApplicationRunner seeder = new DemoDataSeeder().seedDemoData(
                mock(UserRepository.class),
                mock(ProductRepository.class),
                mock(PasswordEncoder.class),
                "admin@order-support.local",
                "");

        assertThatThrownBy(() -> seeder.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("demo.admin.password must be set when demo.data.enabled=true");
    }
}
