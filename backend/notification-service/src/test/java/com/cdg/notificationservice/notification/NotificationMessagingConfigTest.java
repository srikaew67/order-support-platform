package com.cdg.notificationservice.notification;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;

class NotificationMessagingConfigTest {
    private final NotificationMessagingConfig config = new NotificationMessagingConfig();
    @Test void durablePrimaryQueueRoutesRejectedMessagesToDeadLetterQueue() {
        Queue primary = config.notificationQueue();
        Queue dead = config.deadLetterQueue();
        assertTrue(primary.isDurable());
        assertEquals("notifications.dlx", primary.getArguments().get("x-dead-letter-exchange"));
        assertEquals("notification.dead", primary.getArguments().get("x-dead-letter-routing-key"));
        assertTrue(dead.isDurable());
        Binding created = config.orderCreatedBinding(primary, config.orderEventsExchange());
        assertEquals("order.created", created.getRoutingKey());
        assertEquals("order.status.changed",
                config.orderStatusBinding(primary, config.orderEventsExchange()).getRoutingKey());
        assertEquals("ticket.status.changed",
                config.ticketStatusBinding(primary, config.ticketEventsExchange()).getRoutingKey());
        assertEquals("notification.dead", config.deadLetterBinding(dead, config.deadLetterExchange()).getRoutingKey());
    }
}
