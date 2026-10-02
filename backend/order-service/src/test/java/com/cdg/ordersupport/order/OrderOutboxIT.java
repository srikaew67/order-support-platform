package com.cdg.ordersupport.order;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cdg.ordersupport.product.Product;
import com.cdg.ordersupport.product.ProductRepository;
import com.cdg.ordersupport.security.JwtTokenService;
import com.cdg.ordersupport.user.Role;
import com.cdg.ordersupport.user.User;
import com.cdg.ordersupport.user.UserRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
class OrderOutboxIT {
    @Autowired MockMvc mockMvc;
    @Autowired ProductRepository products;
    @Autowired UserRepository users;
    @Autowired JwtTokenService tokens;
    @Autowired JdbcTemplate jdbc;
    @Autowired OrderOutboxDispatcher dispatcher;
    @MockBean RabbitTemplate rabbit;

    @BeforeEach void clean() {
        jdbc.update("DELETE FROM order_outbox");
        jdbc.update("DELETE FROM order_items");
        jdbc.update("DELETE FROM orders");
        products.deleteAll();
    }

    @AfterEach void cleanAfter() { clean(); }

    @Test void brokerFailureLeavesCommittedEventPendingForReplay() throws Exception {
        Product product = products.save(new Product("OUTBOX", "Outbox", "", BigDecimal.TEN, 2));
        User customer = users.save(new User(UUID.randomUUID() + "@example.com", "hash", "Customer", Role.CUSTOMER));
        doThrow(new IllegalStateException("broker down")).when(rabbit)
                .convertAndSend(eq("order.events"), eq("order.created"), any(OrderEvent.class),
                        any(MessagePostProcessor.class), any(CorrelationData.class));
        mockMvc.perform(post("/api/v1/orders")
                .header("Authorization", "Bearer " + tokens.create(customer))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[{\"productId\":\"" + product.getId() + "\",\"quantity\":1}]}"))
                .andExpect(status().isCreated());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM order_outbox WHERE published_at IS NULL", Integer.class));
        UUID eventId = jdbc.queryForObject("SELECT event_id FROM order_outbox", UUID.class);

        reset(rabbit);
        doAnswer(call -> {
            CorrelationData confirmation = call.getArgument(4);
            confirmation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).convertAndSend(eq("order.events"), eq("order.created"),
                any(OrderEvent.class), any(MessagePostProcessor.class), any(CorrelationData.class));
        dispatcher.replayPending();
        verify(rabbit).convertAndSend(eq("order.events"), eq("order.created"),
                argThat(event -> ((OrderEvent) event).eventId().equals(eventId)), any(MessagePostProcessor.class),
                any(CorrelationData.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM order_outbox WHERE published_at IS NULL", Integer.class));
        dispatcher.replayPending();
        verifyNoMoreInteractions(rabbit);
    }
}
