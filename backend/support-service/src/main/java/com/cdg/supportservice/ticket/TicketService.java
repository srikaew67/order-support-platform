package com.cdg.supportservice.ticket;

import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class TicketService {
    private final TicketRepository tickets;
    private final TicketCommentRepository comments;
    private final OrderLookupClient orders;
    private final SupportAgentLookupClient agents;
    private final TicketOutboxRepository outbox;
    private final TicketOutboxDispatcher dispatcher;
    public TicketService(TicketRepository tickets, TicketCommentRepository comments,
            OrderLookupClient orders, SupportAgentLookupClient agents,
            TicketOutboxRepository outbox, TicketOutboxDispatcher dispatcher) {
        this.tickets = tickets; this.comments = comments; this.orders = orders; this.agents = agents;
        this.outbox = outbox; this.dispatcher = dispatcher;
    }
    @Transactional public TicketResponse create(TicketRequests.Create request, Authentication auth, String authorization) {
        UUID customer = userId(auth);
        if (request.orderId() != null) orders.validate(request.orderId(), customer, authorization);
        return response(tickets.save(new SupportTicket(customer, request.orderId(),
                request.subject().trim(), request.description().trim())));
    }
    @Transactional(readOnly = true) public TicketPage list(int page, int size, Authentication auth) {
        if (page < 0 || size < 1 || size > 100) throw new TicketException(HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR", "Invalid page or size");
        var pageable = PageRequest.of(page, size);
        var result = staff(auth) ? tickets.findAllByOrderByCreatedAtDesc(pageable)
                : tickets.findByCustomerIdOrderByCreatedAtDesc(userId(auth), pageable);
        return TicketPage.from(result.map(this::response));
    }
    @Transactional(readOnly = true) public TicketResponse get(UUID id, Authentication auth) {
        return response(visible(id, auth));
    }
    @Transactional public TicketResponse update(UUID id, TicketRequests.Update request,
            Authentication auth, String authorization, String correlationId) {
        if (!staff(auth)) throw new TicketException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied");
        SupportTicket ticket = tickets.findForUpdate(id).orElseThrow(() -> missing());
        if (request.status() == null && request.assigneeId() == null && !Boolean.TRUE.equals(request.assignToMe()))
            throw new TicketException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Status or assignee is required");
        UUID assignee = Boolean.TRUE.equals(request.assignToMe()) ? userId(auth) : request.assigneeId();
        if (assignee != null) {
            if (auth.getAuthorities().stream().noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))
                    && !assignee.equals(userId(auth)))
                throw new TicketException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Support agents can only assign themselves");
            agents.validate(assignee, authorization);
        }
        if (request.status() != null && request.status() != ticket.getStatus()) {
            if (!allowed(ticket.getStatus(), request.status()))
                throw new TicketException(HttpStatus.CONFLICT, "INVALID_TRANSITION", "Invalid ticket status transition");
            ticket.transition(request.status());
            TicketEvent event = TicketEvent.statusChanged(ticket, correlationId);
            outbox.save(new TicketOutboxEntry(event));
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { dispatcher.tryPublish(event.eventId()); }
            });
        }
        if (assignee != null) ticket.assign(assignee);
        return response(ticket);
    }
    @Transactional public TicketResponse comment(UUID id, TicketRequests.Comment request, Authentication auth) {
        SupportTicket ticket = visible(id, auth);
        comments.save(new TicketComment(ticket.getId(), userId(auth), request.body().trim()));
        return response(ticket);
    }
    private SupportTicket visible(UUID id, Authentication auth) {
        SupportTicket ticket = tickets.findById(id).orElseThrow(() -> missing());
        if (!staff(auth) && !ticket.getCustomerId().equals(userId(auth)))
            throw new TicketException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied");
        return ticket;
    }
    private TicketResponse response(SupportTicket ticket) {
        return TicketResponse.from(ticket, comments.findByTicketIdOrderByCreatedAtAsc(ticket.getId()));
    }
    private static boolean allowed(TicketStatus current, TicketStatus next) {
        return switch (current) {
            case OPEN -> next == TicketStatus.IN_PROGRESS;
            case IN_PROGRESS -> next == TicketStatus.RESOLVED;
            case RESOLVED -> next == TicketStatus.CLOSED || next == TicketStatus.IN_PROGRESS;
            case CLOSED -> false;
        };
    }
    private static TicketException missing() { return new TicketException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Ticket not found"); }
    private static UUID userId(Authentication auth) { return UUID.fromString(auth.getName()); }
    private static boolean staff(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_SUPPORT")
                || authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
