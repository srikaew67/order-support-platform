package com.cdg.supportservice.ticket;

import java.time.Instant;
import java.util.UUID;

public record TicketEvent(UUID eventId, String eventType, int version, Instant occurredAt,
        UUID ticketId, UUID customerId, String status, String correlationId) {
    public static TicketEvent statusChanged(SupportTicket ticket, String correlationId) {
        return new TicketEvent(UUID.randomUUID(), "TicketStatusChanged", 1, Instant.now(),
                ticket.getId(), ticket.getCustomerId(), ticket.getStatus().name(), correlationId);
    }
}
