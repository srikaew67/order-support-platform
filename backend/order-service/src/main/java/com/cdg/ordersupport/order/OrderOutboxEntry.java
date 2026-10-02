package com.cdg.ordersupport.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_outbox")
public class OrderOutboxEntry {
    @Id @Column(name = "event_id") private UUID eventId;
    @Column(name = "event_type", nullable = false) private String eventType;
    @Column(name = "event_version", nullable = false) private int eventVersion;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(name = "order_id", nullable = false) private UUID orderId;
    @Column(name = "customer_id", nullable = false) private UUID customerId;
    @Column(nullable = false) private String status;
    @Column(name = "total_amount", nullable = false) private BigDecimal totalAmount;
    @Column(name = "correlation_id", nullable = false) private String correlationId;
    @Column(name = "published_at") private Instant publishedAt;

    protected OrderOutboxEntry() {}

    public OrderOutboxEntry(OrderEvent event) {
        this.eventId = event.eventId();
        this.eventType = event.eventType();
        this.eventVersion = event.version();
        this.occurredAt = event.occurredAt();
        this.orderId = event.orderId();
        this.customerId = event.customerId();
        this.status = event.status();
        this.totalAmount = event.totalAmount();
        this.correlationId = event.correlationId();
    }

    public UUID getEventId() { return eventId; }
    public void markPublished() { publishedAt = Instant.now(); }
    public OrderEvent toEvent() {
        return new OrderEvent(eventId, eventType, eventVersion, occurredAt, orderId,
                customerId, status, totalAmount, correlationId);
    }
}
