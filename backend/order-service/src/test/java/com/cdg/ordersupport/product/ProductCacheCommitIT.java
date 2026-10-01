package com.cdg.ordersupport.product;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cdg.ordersupport.security.JwtTokenService;
import com.cdg.ordersupport.user.Role;
import com.cdg.ordersupport.user.User;
import com.cdg.ordersupport.user.UserRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductCacheCommitIT {
    @Autowired MockMvc mockMvc;
    @Autowired ProductRepository products;
    @Autowired UserRepository users;
    @Autowired JwtTokenService tokens;
    @Autowired ProductService productService;
    @Autowired PlatformTransactionManager transactions;
    @MockBean ProductCache cache;

    @Test void interleavedReadCannotRepopulateOldProductAfterWriteCommits() throws Exception {
        Product product = products.save(new Product("RACE", "Before", "", BigDecimal.TEN, 2));
        User admin = users.save(new User(UUID.randomUUID() + "@example.com", "hash", "Admin", Role.ADMIN));
        String authorization = "Bearer " + tokens.create(admin);
        Map<UUID, ProductResponse> values = new ConcurrentHashMap<>();
        CountDownLatch invalidated = new CountDownLatch(1);
        CountDownLatch releaseWriter = new CountDownLatch(1);
        when(cache.readProduct(any(UUID.class))).thenAnswer(call -> Optional.ofNullable(values.get(call.getArgument(0))));
        doAnswer(call -> { values.put(call.getArgument(0), call.getArgument(1)); return null; })
                .when(cache).writeProduct(any(UUID.class), any(ProductResponse.class));
        doAnswer(call -> {
            values.clear();
            invalidated.countDown();
            assertTrue(releaseWriter.await(10, TimeUnit.SECONDS));
            return null;
        }).when(cache).invalidateAll();

        mockMvc.perform(get("/api/v1/products/" + product.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Before"));
        try (var executor = Executors.newSingleThreadExecutor()) {
            var write = executor.submit(() -> mockMvc.perform(put("/api/v1/products/" + product.getId())
                    .header("Authorization", authorization)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"sku\":\"RACE\",\"name\":\"After\",\"price\":10,\"stockQuantity\":2}"))
                    .andExpect(status().isOk()).andReturn());
            try {
                assertTrue(invalidated.await(10, TimeUnit.SECONDS), "write never reached cache invalidation");
                mockMvc.perform(get("/api/v1/products/" + product.getId()))
                        .andExpect(status().isOk());
            } finally {
                releaseWriter.countDown();
            }
            write.get(10, TimeUnit.SECONDS);
        }
        mockMvc.perform(get("/api/v1/products/" + product.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("After"));
    }

    @Test void rolledBackWriteDoesNotInvalidateCache() {
        Product product = products.save(new Product("ROLLBACK", "Before", "", BigDecimal.TEN, 2));
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            productService.update(product.getId(), new CreateProductRequest(
                    "ROLLBACK", "After", "", BigDecimal.TEN, 2));
            status.setRollbackOnly();
        });
        verify(cache, never()).invalidateAll();
        org.junit.jupiter.api.Assertions.assertEquals("Before", products.findById(product.getId()).orElseThrow().getName());
    }
}
