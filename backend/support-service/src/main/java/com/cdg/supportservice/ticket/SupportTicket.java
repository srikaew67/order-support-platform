package com.cdg.supportservice.ticket;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "support_tickets")
public class SupportTicket {
    @Id private UUID id;
    @Column(name = "customer_id", nullable = false) private UUID customerId;
    @Column(name = "order_id") private UUID orderId;
    @Column(nullable = false) private String subject;
    @Column(nullable = false) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TicketStatus status;
    @Column(name = "assignee_id") private UUID assigneeId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected SupportTicket() {}
    public SupportTicket(UUID customerId, UUID orderId, String subject, String description) {
        this.id = UUID.randomUUID(); this.customerId = customerId; this.orderId = orderId;
        this.subject = subject; this.description = description; this.status = TicketStatus.OPEN;
        this.createdAt = Instant.now(); this.updatedAt = createdAt;
    }
    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public UUID getOrderId() { return orderId; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public TicketStatus getStatus() { return status; }
    public UUID getAssigneeId() { return assigneeId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void assign(UUID assigneeId) { this.assigneeId = assigneeId; this.updatedAt = Instant.now(); }
    public void transition(TicketStatus next) { this.status = next; this.updatedAt = Instant.now(); }
}
