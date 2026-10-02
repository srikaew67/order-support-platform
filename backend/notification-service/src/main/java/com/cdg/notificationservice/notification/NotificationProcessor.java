package com.cdg.notificationservice.notification;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationProcessor {
    private final NotificationRepository notifications;
    public NotificationProcessor(NotificationRepository notifications) { this.notifications = notifications; }
    @Transactional(readOnly = true) public boolean alreadyProcessed(UUID eventId) {
        return notifications.existsByEventId(eventId);
    }
    @Transactional public void persist(NotificationEvent event) {
        if (notifications.existsByEventId(event.eventId())) return;
        String text = switch (event.eventType()) {
            case "OrderCreated" -> "Order " + event.subjectId() + " was placed";
            case "OrderStatusChanged" -> "Order " + event.subjectId() + " is " + event.status();
            case "TicketStatusChanged" -> "Ticket " + event.subjectId() + " is " + event.status();
            default -> throw new InvalidNotificationEventException("Unsupported event type");
        };
        notifications.saveAndFlush(new Notification(event.eventId(), event.customerId(),
                event.eventType(), event.subjectId(), text, event.correlationId()));
    }
}
