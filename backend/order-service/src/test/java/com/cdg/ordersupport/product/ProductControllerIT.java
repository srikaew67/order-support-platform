package com.cdg.ordersupport.product;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cdg.ordersupport.security.JwtTokenService;
import com.cdg.ordersupport.user.Role;
import com.cdg.ordersupport.user.User;
import com.cdg.ordersupport.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerIT {
    @Autowired MockMvc mockMvc;
    @Autowired ProductRepository products;
    @Autowired UserRepository users;
    @Autowired JwtTokenService tokens;
    @Autowired ObjectMapper json;

    @BeforeEach void cleanProducts() { products.deleteAll(); }

    private String token(Role role) {
        User user = users.save(new User(UUID.randomUUID() + "@example.com", "hash", "Test", role));
        return "Bearer " + tokens.create(user);
    }

    @Test void catalogIsPublicAndPaginated() throws Exception {
        products.save(new Product("A", "Apple", "", BigDecimal.TEN, 2));
        products.save(new Product("B", "Banana", "", BigDecimal.TEN, 2));
        products.save(new Product("C", "Cherry", "", BigDecimal.TEN, 2));
        mockMvc.perform(get("/api/v1/products?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].name").value("Cherry"));
    }

    @Test void productDetailReturnsProductAndMissingIs404() throws Exception {
        Product saved = products.save(new Product("DETAIL", "Detail", "Description", BigDecimal.TEN, 2));
        mockMvc.perform(get("/api/v1/products/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Description"));
        mockMvc.perform(get("/api/v1/products/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test void adminCanCreateUpdateAndSoftDeleteProduct() throws Exception {
        String admin = token(Role.ADMIN);
        String created = mockMvc.perform(post("/api/v1/products").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"SKU-1\",\"name\":\"Original\",\"description\":\"Old\",\"price\":10,\"stockQuantity\":4}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Original"))
                .andReturn().getResponse().getContentAsString();
        String id = json.readTree(created).get("id").asText();
        mockMvc.perform(put("/api/v1/products/" + id).header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"SKU-1\",\"name\":\"Updated\",\"description\":\"New\",\"price\":12,\"stockQuantity\":3,\"version\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated"))
                .andExpect(jsonPath("$.version").value(1));
        mockMvc.perform(get("/api/v1/products/" + id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated"));
        mockMvc.perform(delete("/api/v1/products/" + id).header("Authorization", admin))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/products/" + id)).andExpect(status().isNotFound());
    }

    @Test void invalidProductInputHasFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/products").header("Authorization", token(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"\",\"name\":\"\",\"price\":-1,\"stockQuantity\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.sku").exists())
                .andExpect(jsonPath("$.fieldErrors.price").exists());
    }

    @Test void customerCannotWriteProducts() throws Exception {
        String customer = token(Role.CUSTOMER);
        Product existing = products.save(new Product("EXISTING", "Existing", "", BigDecimal.TEN, 2));
        mockMvc.perform(post("/api/v1/products").header("Authorization", customer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"NO\",\"name\":\"No\",\"price\":1,\"stockQuantity\":1}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/products/" + existing.getId()).header("Authorization", customer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"EXISTING\",\"name\":\"Changed\",\"price\":1,\"stockQuantity\":1}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/products/" + existing.getId()).header("Authorization", customer))
                .andExpect(status().isForbidden());
    }

    @Test void duplicateSkuAndInvalidPageAreRejected() throws Exception {
        products.save(new Product("DUP", "First", "", BigDecimal.TEN, 2));
        mockMvc.perform(post("/api/v1/products").header("Authorization", token(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"DUP\",\"name\":\"Second\",\"price\":1,\"stockQuantity\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
        mockMvc.perform(get("/api/v1/products?size=0"))
                .andExpect(status().isBadRequest());
    }

    @Test void malformedPageSizeAndIdUseStandardErrors() throws Exception {
        for (String path : java.util.List.of(
                "/api/v1/products?page=abc", "/api/v1/products?size=abc", "/api/v1/products/not-a-uuid")) {
            mockMvc.perform(get(path).header("X-Correlation-ID", "catalog-bad-input"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.correlationId").value("catalog-bad-input"));
        }
    }

    @Test void priceMustFitDatabasePrecisionAndScale() throws Exception {
        String admin = token(Role.ADMIN);
        for (String price : java.util.List.of("12.345", "12345678901.00")) {
            mockMvc.perform(post("/api/v1/products").header("Authorization", admin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"sku\":\"PRICE-" + price + "\",\"name\":\"Price\",\"price\":" + price + ",\"stockQuantity\":1}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.fieldErrors.price").exists());
        }
    }
}
