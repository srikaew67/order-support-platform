package com.cdg.supportservice.ticket;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TicketResponse(UUID id, UUID customerId, UUID orderId, String subject, String description,
        TicketStatus status, UUID assigneeId, Instant createdAt, Instant updatedAt, List<Comment> comments) {
    public record Comment(UUID id, UUID authorId, String body, Instant createdAt) {}
    public static TicketResponse from(SupportTicket ticket, List<TicketComment> comments) {
        return new TicketResponse(ticket.getId(), ticket.getCustomerId(), ticket.getOrderId(),
                ticket.getSubject(), ticket.getDescription(), ticket.getStatus(), ticket.getAssigneeId(),
                ticket.getCreatedAt(), ticket.getUpdatedAt(), comments.stream()
                        .map(comment -> new Comment(comment.getId(), comment.getAuthorId(), comment.getBody(), comment.getCreatedAt())).toList());
    }
}
