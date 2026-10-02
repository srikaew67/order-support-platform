package com.cdg.supportservice.ticket;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class OrderLookupClientTest {
    private OrderLookupClient client;
    private MockRestServiceServer server;
    private final UUID order = UUID.randomUUID();
    private final UUID customer = UUID.randomUUID();
    @BeforeEach void setup() {
        client = new OrderLookupClient(new RestTemplateBuilder(), "http://orders");
        RestTemplate http = new RestTemplate();
        server = MockRestServiceServer.bindTo(http).build();
        ReflectionTestUtils.setField(client, "http", http);
    }
    @Test void forwardsBearerTokenAndAcceptsOwnedOrder() {
        server.expect(requestTo("http://orders/api/v1/orders/" + order))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer customer-token"))
                .andRespond(withSuccess("{\"customerId\":\"" + customer + "\"}", MediaType.APPLICATION_JSON));
        assertDoesNotThrow(() -> client.validate(order, customer, "Bearer customer-token"));
        server.verify();
    }
    @Test void rejectsOrderOwnedByAnotherCustomer() {
        server.expect(requestTo("http://orders/api/v1/orders/" + order))
                .andRespond(withSuccess("{\"customerId\":\"" + UUID.randomUUID() + "\"}", MediaType.APPLICATION_JSON));
        TicketException exception = assertThrows(TicketException.class,
                () -> client.validate(order, customer, "Bearer customer-token"));
        assertEquals(HttpStatus.FORBIDDEN, exception.status());
    }
    @Test void rejectsMissingOrderWithoutCreatingTicket() {
        server.expect(requestTo("http://orders/api/v1/orders/" + order)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        TicketException exception = assertThrows(TicketException.class,
                () -> client.validate(order, customer, "Bearer customer-token"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
    }
}
