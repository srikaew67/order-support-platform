package com.cdg.supportservice.ticket;

import com.cdg.supportservice.common.CorrelationIdFilter;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {
    private final TicketService tickets;
    public TicketController(TicketService tickets) { this.tickets = tickets; }
    @PostMapping @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody TicketRequests.Create request,
            Authentication auth, @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tickets.create(request, auth, authorization));
    }
    @GetMapping public TicketPage list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, Authentication auth) {
        return tickets.list(page, size, auth);
    }
    @GetMapping("/{id}") public TicketResponse get(@PathVariable UUID id, Authentication auth) {
        return tickets.get(id, auth);
    }
    @PatchMapping("/{id}") @PreAuthorize("hasAnyRole('SUPPORT', 'ADMIN')")
    public TicketResponse update(@PathVariable UUID id, @Valid @RequestBody TicketRequests.Update request,
            Authentication auth, @RequestHeader("Authorization") String authorization,
            @RequestAttribute(CorrelationIdFilter.ATTRIBUTE) String correlationId) {
        return tickets.update(id, request, auth, authorization, correlationId);
    }
    @PostMapping("/{id}/comments")
    public ResponseEntity<TicketResponse> comment(@PathVariable UUID id, @Valid @RequestBody TicketRequests.Comment request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tickets.comment(id, request, auth));
    }
}
