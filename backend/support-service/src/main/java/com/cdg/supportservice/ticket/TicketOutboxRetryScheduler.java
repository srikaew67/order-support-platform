package com.cdg.supportservice.ticket;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tickets.outbox.retry-enabled", havingValue = "true", matchIfMissing = true)
public class TicketOutboxRetryScheduler {
    private final TicketOutboxDispatcher dispatcher;
    public TicketOutboxRetryScheduler(TicketOutboxDispatcher dispatcher) { this.dispatcher = dispatcher; }
    @Scheduled(initialDelayString = "${tickets.outbox.initial-delay-ms:5000}",
            fixedDelayString = "${tickets.outbox.retry-delay-ms:5000}")
    public void retry() { dispatcher.replayPending(); }
}
