package com.cdg.ordersupport.order;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class OrderOutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(OrderOutboxDispatcher.class);
    private final OrderOutboxRepository outbox;
    private final OrderEventPublisher publisher;
    private final TransactionTemplate transactions;

    public OrderOutboxDispatcher(OrderOutboxRepository outbox, OrderEventPublisher publisher,
            PlatformTransactionManager manager) {
        this.outbox = outbox;
        this.publisher = publisher;
        this.transactions = new TransactionTemplate(manager);
        this.transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void tryPublish(UUID eventId) {
        try {
            transactions.executeWithoutResult(status -> outbox.findPendingForUpdate(eventId).ifPresent(entry -> {
                publisher.publish(entry.toEvent());
                entry.markPublished();
            }));
        } catch (RuntimeException exception) {
            log.warn("Order event {} remains pending for retry", eventId, exception);
        }
    }

    public void replayPending() {
        var pending = outbox.findByPublishedAtIsNullOrderByOccurredAtAsc(PageRequest.of(0, 50));
        pending.forEach(entry -> tryPublish(entry.getEventId()));
    }
}
