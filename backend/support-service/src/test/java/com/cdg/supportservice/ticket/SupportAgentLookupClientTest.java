package com.cdg.supportservice.ticket;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.time.Duration;
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

class SupportAgentLookupClientTest {
    private SupportAgentLookupClient client;
    private MockRestServiceServer server;
    private final UUID agent = UUID.randomUUID();
    @BeforeEach void setup() {
        client = new SupportAgentLookupClient(new RestTemplateBuilder(), "http://orders",
                Duration.ofSeconds(2), Duration.ofSeconds(3));
        RestTemplate http = new RestTemplate();
        server = MockRestServiceServer.bindTo(http).build();
        ReflectionTestUtils.setField(client, "http", http);
    }
    @Test void acceptsSupportIdentityAndForwardsStaffToken() {
        server.expect(requestTo("http://orders/api/v1/support-agents/" + agent))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer staff-token"))
                .andRespond(withSuccess("{\"id\":\"" + agent + "\"}", MediaType.APPLICATION_JSON));
        assertDoesNotThrow(() -> client.validate(agent, "Bearer staff-token"));
        server.verify();
    }
    @Test void missingOrWrongRoleIdentityIsInvalidAssignee() {
        server.expect(requestTo("http://orders/api/v1/support-agents/" + agent))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        TicketException error = assertThrows(TicketException.class,
                () -> client.validate(agent, "Bearer staff-token"));
        assertEquals("INVALID_ASSIGNEE", error.code());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.status());
    }
}
