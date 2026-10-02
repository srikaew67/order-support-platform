package com.cdg.notificationservice.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications", schema = "notification")
public class Notification {
    @Id private UUID id;
    @Column(name = "event_id", nullable = false, unique = true) private UUID eventId;
    @Column(name = "customer_id", nullable = false) private UUID customerId;
    @Column(name = "event_type", nullable = false) private String eventType;
    @Column(name = "subject_id", nullable = false) private UUID subjectId;
    @Column(nullable = false) private String message;
    @Column(name = "correlation_id", nullable = false) private String correlationId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected Notification() {}
    public Notification(UUID eventId, UUID customerId, String eventType, UUID subjectId,
            String message, String correlationId) {
        id = UUID.randomUUID(); this.eventId = eventId; this.customerId = customerId;
        this.eventType = eventType; this.subjectId = subjectId; this.message = message;
        this.correlationId = correlationId; this.createdAt = Instant.now();
    }
    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public UUID getCustomerId() { return customerId; }
    public String getEventType() { return eventType; }
    public UUID getSubjectId() { return subjectId; }
    public String getMessage() { return message; }
    public String getCorrelationId() { return correlationId; }
    public Instant getCreatedAt() { return createdAt; }
}
