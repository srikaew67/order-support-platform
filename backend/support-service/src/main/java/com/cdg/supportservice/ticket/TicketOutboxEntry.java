package com.cdg.supportservice.ticket;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_outbox")
public class TicketOutboxEntry {
    @Id @Column(name = "event_id") private UUID eventId;
    @Column(name = "event_type", nullable = false) private String eventType;
    @Column(name = "event_version", nullable = false) private int eventVersion;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(name = "ticket_id", nullable = false) private UUID ticketId;
    @Column(name = "customer_id", nullable = false) private UUID customerId;
    @Column(nullable = false) private String status;
    @Column(name = "correlation_id", nullable = false) private String correlationId;
    @Column(name = "published_at") private Instant publishedAt;
    protected TicketOutboxEntry() {}
    public TicketOutboxEntry(TicketEvent event) {
        eventId = event.eventId(); eventType = event.eventType(); eventVersion = event.version();
        occurredAt = event.occurredAt(); ticketId = event.ticketId(); customerId = event.customerId();
        status = event.status(); correlationId = event.correlationId();
    }
    public UUID getEventId() { return eventId; }
    public TicketEvent toEvent() { return new TicketEvent(eventId, eventType, eventVersion,
            occurredAt, ticketId, customerId, status, correlationId); }
    public void markPublished() { publishedAt = Instant.now(); }
}
