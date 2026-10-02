package com.cdg.ordersupport.order;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import com.cdg.ordersupport.product.Product;
import com.cdg.ordersupport.product.ProductRepository;
import com.cdg.ordersupport.security.JwtTokenService;
import com.cdg.ordersupport.user.Role;
import com.cdg.ordersupport.user.User;
import com.cdg.ordersupport.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerIT {
    @Autowired MockMvc mockMvc;
    @Autowired ProductRepository products;
    @Autowired UserRepository users;
    @Autowired JwtTokenService tokens;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @MockBean RabbitTemplate rabbit;

    @BeforeEach void clean() {
        clearData();
        doAnswer(call -> {
            CorrelationData confirmation = call.getArgument(4);
            confirmation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).convertAndSend(anyString(), anyString(), any(OrderEvent.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    private void clearData() {
        jdbc.update("DELETE FROM order_outbox");
        jdbc.update("DELETE FROM order_items");
        jdbc.update("DELETE FROM orders");
        products.deleteAll();
    }

    @AfterEach void cleanAfter() { clearData(); }

    private String token(Role role) {
        User user = users.save(new User(UUID.randomUUID() + "@example.com", "hash", "Test", role));
        return "Bearer " + tokens.create(user);
    }

    private String create(String token, String items) throws Exception {
        String response = mockMvc.perform(post("/api/v1/orders").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"items\":" + items + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asText();
    }

    @Test void creationReservesStockAndSnapshotsPrice() throws Exception {
        Product product = products.save(new Product("ORDER-ONE", "Desk", "", new BigDecimal("12.50"), 5));
        String id = create(token(Role.CUSTOMER), "[{\"productId\":\"" + product.getId() + "\",\"quantity\":2}]");
        Product repriced = products.findById(product.getId()).orElseThrow();
        repriced.update("ORDER-ONE", "Desk", "", new BigDecimal("30.00"), 3);
        products.save(repriced);
        mockMvc.perform(get("/api/v1/orders/" + id).header("Authorization", token(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber", org.hamcrest.Matchers.matchesPattern("ORD-[0-9A-F-]{36}")))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(12.50))
                .andExpect(jsonPath("$.items[0].subtotal").value(25.00))
                .andExpect(jsonPath("$.totalAmount").value(25.00));
        assertEquals(3, products.findById(product.getId()).orElseThrow().getStockQuantity());
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM order_outbox WHERE published_at IS NOT NULL", Integer.class));
        ArgumentCaptor<OrderEvent> event = ArgumentCaptor.forClass(OrderEvent.class);
        verify(rabbit).convertAndSend(eq("order.events"), eq("order.created"), event.capture(),
                any(MessagePostProcessor.class), any(CorrelationData.class));
        assertNotNull(event.getValue().eventId());
        assertEquals("OrderCreated", event.getValue().eventType());
        assertEquals(1, event.getValue().version());
        assertEquals(UUID.fromString(id), event.getValue().orderId());
        assertNotNull(event.getValue().correlationId());
    }

    @Test void insufficientSecondItemRollsBackAllStockAndOrderRows() throws Exception {
        Product first = products.save(new Product("FIRST", "First", "", BigDecimal.TEN, 4));
        Product second = products.save(new Product("SECOND", "Second", "", BigDecimal.TEN, 1));
        String items = "[{\"productId\":\"" + first.getId() + "\",\"quantity\":2},"
                + "{\"productId\":\"" + second.getId() + "\",\"quantity\":2}]";
        mockMvc.perform(post("/api/v1/orders").header("Authorization", token(Role.CUSTOMER))
                .contentType(MediaType.APPLICATION_JSON).content("{\"items\":" + items + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
        assertEquals(4, products.findById(first.getId()).orElseThrow().getStockQuantity());
        assertEquals(1, products.findById(second.getId()).orElseThrow().getStockQuantity());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM order_outbox", Integer.class));
        verify(rabbit, never()).convertAndSend(anyString(), anyString(), any(OrderEvent.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    @Test void customerCannotReadOrCancelAnotherCustomersOrder() throws Exception {
        Product product = products.save(new Product("OWNED", "Owned", "", BigDecimal.TEN, 3));
        String owner = token(Role.CUSTOMER);
        String other = token(Role.CUSTOMER);
        String id = create(owner, "[{\"productId\":\"" + product.getId() + "\",\"quantity\":1}]");
        mockMvc.perform(get("/api/v1/orders/" + id).header("Authorization", other))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/orders/" + id + "/cancel").header("Authorization", other))
                .andExpect(status().isForbidden());
        assertEquals(2, products.findById(product.getId()).orElseThrow().getStockQuantity());
    }

    @Test void cancellationIsIdempotentAndRestoresStockOnlyOnce() throws Exception {
        Product product = products.save(new Product("CANCEL", "Cancel", "", BigDecimal.TEN, 3));
        String customer = token(Role.CUSTOMER);
        String id = create(customer, "[{\"productId\":\"" + product.getId() + "\",\"quantity\":2}]");
        mockMvc.perform(post("/api/v1/orders/" + id + "/cancel").header("Authorization", customer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(post("/api/v1/orders/" + id + "/cancel").header("Authorization", customer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        assertEquals(3, products.findById(product.getId()).orElseThrow().getStockQuantity());
        verify(rabbit, times(1)).convertAndSend(eq("order.events"), eq("order.status.changed"),
                any(OrderEvent.class), any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    @Test void adminCanAdvanceStatusButCustomerCannot() throws Exception {
        Product product = products.save(new Product("STATUS", "Status", "", BigDecimal.TEN, 3));
        String customer = token(Role.CUSTOMER);
        String id = create(customer, "[{\"productId\":\"" + product.getId() + "\",\"quantity\":1}]");
        String body = "{\"status\":\"PROCESSING\"}";
        mockMvc.perform(patch("/api/v1/orders/" + id + "/status").header("Authorization", customer)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        String admin = token(Role.ADMIN);
        mockMvc.perform(patch("/api/v1/orders/" + id + "/status").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PROCESSING"));
        mockMvc.perform(patch("/api/v1/orders/" + id + "/status").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SHIPPED"));
        mockMvc.perform(post("/api/v1/orders/" + id + "/cancel").header("Authorization", customer))
                .andExpect(status().isConflict());
        verify(rabbit, times(2)).convertAndSend(eq("order.events"), eq("order.status.changed"),
                any(OrderEvent.class), any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    @Test void invalidOrderInputAndStatusTransitionAreRejected() throws Exception {
        String customer = token(Role.CUSTOMER);
        mockMvc.perform(post("/api/v1/orders").header("Authorization", customer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.items").exists());
        Product product = products.save(new Product("VALIDATE", "Validate", "", BigDecimal.TEN, 2));
        mockMvc.perform(post("/api/v1/orders").header("Authorization", customer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[{\"productId\":\"" + product.getId() + "\",\"quantity\":0}]}"))
                .andExpect(status().isBadRequest());
        String id = create(customer, "[{\"productId\":\"" + product.getId() + "\",\"quantity\":1}]");
        mockMvc.perform(patch("/api/v1/orders/" + id + "/status")
                .header("Authorization", token(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }

    @Test void customerListIsPaginatedAndScopedToOwner() throws Exception {
        Product product = products.save(new Product("LIST", "List", "", BigDecimal.TEN, 4));
        String owner = token(Role.CUSTOMER);
        String other = token(Role.CUSTOMER);
        create(owner, "[{\"productId\":\"" + product.getId() + "\",\"quantity\":1}]");
        mockMvc.perform(get("/api/v1/orders?page=0&size=1").header("Authorization", owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/orders?page=0&size=1").header("Authorization", other))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test void staleAdminEditCannotRestoreStockReservedByAnOrder() throws Exception {
        Product product = products.save(new Product("STALE", "Before", "", BigDecimal.TEN, 5));
        String id = create(token(Role.CUSTOMER), "[{\"productId\":\"" + product.getId() + "\",\"quantity\":2}]");
        assertNotNull(id);
        mockMvc.perform(put("/api/v1/products/" + product.getId())
                .header("Authorization", token(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"STALE\",\"name\":\"After\",\"price\":20,\"stockQuantity\":5,\"version\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_PRODUCT"));
        assertEquals(3, products.findById(product.getId()).orElseThrow().getStockQuantity());
    }
}
