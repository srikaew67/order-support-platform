package com.cdg.ordersupport.order;

import com.cdg.ordersupport.common.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {
    public static final String EXCHANGE = "order.events";
    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);
    private final RabbitTemplate rabbit;

    public OrderEventPublisher(RabbitTemplate rabbit) { this.rabbit = rabbit; }

    public void publish(OrderEvent event) {
        String key = event.eventType().equals("OrderCreated") ? "order.created" : "order.status.changed";
        try {
            rabbit.convertAndSend(EXCHANGE, key, event, message -> {
                message.getMessageProperties().setMessageId(event.eventId().toString());
                message.getMessageProperties().setHeader(CorrelationIdFilter.HEADER, event.correlationId());
                return message;
            });
        } catch (RuntimeException exception) {
            log.error("Unable to publish order event {} for order {}", event.eventId(), event.orderId(), exception);
        }
    }
}
