package com.cdg.notificationservice.notification;

import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {
    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);
    private final NotificationEventParser parser;
    private final NotificationProcessor processor;
    private final RetryTemplate retries;
    public NotificationListener(NotificationEventParser parser, NotificationProcessor processor, RetryTemplate retries) {
        this.parser = parser; this.processor = processor; this.retries = retries;
    }
    @RabbitListener(queues = NotificationMessagingConfig.EVENT_QUEUE)
    public void handle(Message message) {
        Object header = message.getMessageProperties().getHeaders().get("X-Correlation-ID");
        String correlationId = header instanceof String value && !value.isBlank() ? value : "unknown";
        MDC.put("correlationId", correlationId);
        try {
            NotificationEvent event;
            try {
                event = parser.parse(message);
            } catch (InvalidNotificationEventException exception) {
                log.warn("Rejecting malformed event message {} correlationId={} to dead-letter queue",
                        message.getMessageProperties().getMessageId(), correlationId, exception);
                throw new AmqpRejectAndDontRequeueException("Malformed notification event", exception);
            }
            correlationId = event.correlationId();
            MDC.put("correlationId", correlationId);
            retries.execute(context -> {
                try {
                    processor.persist(event);
                } catch (DataIntegrityViolationException exception) {
                    try {
                        if (processor.alreadyProcessed(event.eventId())) {
                            log.info("Notification event {} correlationId={} was already stored",
                                    event.eventId(), event.correlationId());
                            return null;
                        }
                    } catch (RuntimeException lookupFailure) {
                        exception.addSuppressed(lookupFailure);
                    }
                    log.warn("Notification event {} correlationId={} failed on attempt {}",
                            event.eventId(), event.correlationId(), context.getRetryCount() + 1, exception);
                    throw exception;
                } catch (RuntimeException exception) {
                    log.warn("Notification event {} correlationId={} failed on attempt {}",
                            event.eventId(), event.correlationId(), context.getRetryCount() + 1, exception);
                    throw exception;
                }
                return null;
            }, context -> {
                log.error("Notification event {} correlationId={} exhausted retries and is dead-lettered",
                        event.eventId(), event.correlationId(), context.getLastThrowable());
                throw new AmqpRejectAndDontRequeueException("Notification event exhausted retries",
                        context.getLastThrowable());
            });
        } finally {
            MDC.remove("correlationId");
        }
    }
}
