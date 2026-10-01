package com.cdg.ordersupport.product;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductRedisFallbackIT {
    @Autowired MockMvc mockMvc;
    @Autowired ProductRepository products;
    @MockBean StringRedisTemplate redis;

    @BeforeEach void cleanProducts() { products.deleteAll(); }

    @Test void listAndDetailFallBackToDatabaseWhenRedisIsUnavailable() throws Exception {
        Product product = products.save(new Product("FALLBACK", "Fallback product", "", BigDecimal.TEN, 2));
        when(redis.opsForValue()).thenThrow(new RedisConnectionFailureException("Redis unavailable"));
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Fallback product"));
        mockMvc.perform(get("/api/v1/products/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Fallback product"));
    }
}
