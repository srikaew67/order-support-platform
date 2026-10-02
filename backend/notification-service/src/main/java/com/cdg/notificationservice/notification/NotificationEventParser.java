package com.cdg.notificationservice.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.Set;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventParser {
    private final ObjectMapper json;
    public NotificationEventParser(ObjectMapper json) { this.json = json; }
    public NotificationEvent parse(Message message) {
        try {
            JsonNode root = json.readTree(message.getBody());
            if (root == null || !root.isObject() || !root.path("version").isIntegralNumber()
                    || !root.path("version").canConvertToInt() || root.path("version").intValue() != 1)
                throw new InvalidNotificationEventException("Unsupported or missing event version");
            String type = required(root, "event_type", "eventType");
            String exchange = message.getMessageProperties().getReceivedExchange();
            boolean order = (type.equals("OrderCreated") || type.equals("OrderStatusChanged"))
                    && "order.events".equals(exchange);
            boolean ticket = type.equals("TicketStatusChanged") && "ticket.events".equals(exchange);
            if (!order && !ticket) throw new InvalidNotificationEventException("Unsupported event type or exchange");
            UUID eventId = UUID.fromString(required(root, "event_id", "eventId"));
            UUID customerId = UUID.fromString(required(root, "customer_id", "customerId"));
            UUID subjectId = UUID.fromString(order
                    ? required(root, "order_id", "orderId") : required(root, "ticket_id", "ticketId"));
            String status = required(root, "status", "status");
            if (order && !Set.of("PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED").contains(status))
                throw new InvalidNotificationEventException("Unsupported order status");
            if (type.equals("OrderCreated") && !status.equals("PENDING"))
                throw new InvalidNotificationEventException("Invalid created order status");
            if (ticket && !Set.of("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED").contains(status))
                throw new InvalidNotificationEventException("Unsupported ticket status");
            String correlationId = required(root, "correlation_id", "correlationId");
            java.time.Instant.parse(required(root, "occurred_at", "occurredAt"));
            return new NotificationEvent(eventId, type, subjectId, customerId, status, correlationId);
        } catch (InvalidNotificationEventException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidNotificationEventException("Malformed notification event", exception);
        }
    }
    private static String required(JsonNode node, String primary, String alternate) {
        JsonNode value = node.hasNonNull(primary) ? node.get(primary) : node.get(alternate);
        if (value == null || !value.isTextual() || value.asText().isBlank())
            throw new InvalidNotificationEventException("Missing or invalid " + primary);
        return value.asText();
    }
}
