package com.cdg.notificationservice.notification;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class NotificationProcessorIT {
    @Autowired NotificationEventParser parser;
    @Autowired NotificationProcessor processor;
    @Autowired NotificationRepository notifications;
    @BeforeEach void clean() { notifications.deleteAll(); }

    private Message message(String json, String exchange) {
        MessageProperties properties = new MessageProperties();
        properties.setReceivedExchange(exchange);
        return new Message(json.getBytes(StandardCharsets.UTF_8), properties);
    }
    @Test void createsOneNotificationForDuplicateOrderEvent() {
        UUID eventId = UUID.randomUUID();
        UUID customer = UUID.randomUUID();
        String payload = "{\"event_id\":\"" + eventId + "\",\"event_type\":\"OrderCreated\","
                + "\"version\":1,\"occurred_at\":\"2026-10-01T00:00:00Z\","
                + "\"order_id\":\"" + UUID.randomUUID() + "\",\"customer_id\":\"" + customer + "\","
                + "\"status\":\"PENDING\",\"total_amount\":12.50,\"correlation_id\":\"request-1\"}";
        NotificationEvent event = parser.parse(message(payload, "order.events"));
        processor.persist(event);
        processor.persist(event);
        assertEquals(1, notifications.count());
        var saved = notifications.findByEventId(eventId).orElseThrow();
        assertEquals(customer, saved.getCustomerId());
        assertEquals("OrderCreated", saved.getEventType());
        assertEquals("request-1", saved.getCorrelationId());
    }
    @Test void acceptsTicketStatusChangedCamelCasePayload() {
        UUID eventId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        String payload = "{\"eventId\":\"" + eventId + "\",\"eventType\":\"TicketStatusChanged\","
                + "\"version\":1,\"occurredAt\":\"2026-10-01T00:00:00Z\","
                + "\"ticketId\":\"" + ticketId + "\",\"customerId\":\"" + UUID.randomUUID() + "\","
                + "\"status\":\"RESOLVED\",\"correlationId\":\"request-2\"}";
        processor.persist(parser.parse(message(payload, "ticket.events")));
        var saved = notifications.findByEventId(eventId).orElseThrow();
        assertEquals(ticketId, saved.getSubjectId());
        assertEquals("TicketStatusChanged", saved.getEventType());
    }
    @Test void orderStatusChangeCreatesNotificationButUnknownStatusIsRejected() {
        UUID eventId = UUID.randomUUID();
        String base = "{\"event_id\":\"" + eventId + "\",\"event_type\":\"OrderStatusChanged\","
                + "\"version\":1,\"occurred_at\":\"2026-10-01T00:00:00Z\","
                + "\"order_id\":\"" + UUID.randomUUID() + "\",\"customer_id\":\"" + UUID.randomUUID() + "\","
                + "\"status\":\"%s\",\"correlation_id\":\"request-3\"}";
        processor.persist(parser.parse(message(base.formatted("SHIPPED"), "order.events")));
        assertEquals(1, notifications.count());
        assertThrows(InvalidNotificationEventException.class,
                () -> parser.parse(message(base.formatted("UNKNOWN"), "order.events")));
    }
    @Test void rejectsMalformedAndUnsupportedEventsWithoutStoringThem() {
        assertThrows(InvalidNotificationEventException.class,
                () -> parser.parse(message("{bad-json", "order.events")));
        assertThrows(InvalidNotificationEventException.class,
                () -> parser.parse(message("{\"event_id\":\"" + UUID.randomUUID()
                        + "\",\"event_type\":\"OrderCreated\",\"version\":2}", "order.events")));
        assertEquals(0, notifications.count());
    }
}
