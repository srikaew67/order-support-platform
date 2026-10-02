package com.cdg.notificationservice.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
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
        NotificationEvent event;
        try {
            event = parser.parse(message);
        } catch (InvalidNotificationEventException exception) {
            log.warn("Rejecting malformed event message {} to dead-letter queue",
                    message.getMessageProperties().getMessageId(), exception);
            throw new AmqpRejectAndDontRequeueException("Malformed notification event", exception);
        }
        retries.execute(context -> {
            try {
                processor.persist(event);
            } catch (RuntimeException exception) {
                log.warn("Notification event {} failed on attempt {}",
                        event.eventId(), context.getRetryCount() + 1, exception);
                throw exception;
            }
            return null;
        }, context -> {
            log.error("Notification event {} exhausted retries and is dead-lettered", event.eventId(),
                    context.getLastThrowable());
            throw new AmqpRejectAndDontRequeueException("Notification event exhausted retries",
                    context.getLastThrowable());
        });
    }
}
