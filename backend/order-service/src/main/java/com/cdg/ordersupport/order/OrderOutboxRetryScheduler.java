package com.cdg.ordersupport.order;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "orders.outbox.retry-enabled", havingValue = "true", matchIfMissing = true)
public class OrderOutboxRetryScheduler {
    private final OrderOutboxDispatcher dispatcher;
    public OrderOutboxRetryScheduler(OrderOutboxDispatcher dispatcher) { this.dispatcher = dispatcher; }

    @Scheduled(initialDelayString = "${orders.outbox.initial-delay-ms:5000}",
            fixedDelayString = "${orders.outbox.retry-delay-ms:5000}")
    public void retry() { dispatcher.replayPending(); }
}
