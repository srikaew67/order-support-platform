package com.cdg.ordersupport.order;

import com.cdg.ordersupport.common.CorrelationIdFilter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {
    public static final String EXCHANGE = "order.events";
    private final RabbitTemplate rabbit;

    public OrderEventPublisher(RabbitTemplate rabbit) { this.rabbit = rabbit; }

    public void publish(OrderEvent event) {
        String key = event.eventType().equals("OrderCreated") ? "order.created" : "order.status.changed";
        CorrelationData confirmation = new CorrelationData(event.eventId().toString());
        rabbit.convertAndSend(EXCHANGE, key, event, message -> {
            message.getMessageProperties().setMessageId(event.eventId().toString());
            message.getMessageProperties().setHeader(CorrelationIdFilter.HEADER, event.correlationId());
            return message;
        }, confirmation);
        try {
            CorrelationData.Confirm result = confirmation.getFuture().get(5, TimeUnit.SECONDS);
            if (!result.isAck() || confirmation.getReturned() != null) {
                throw new IllegalStateException("Broker did not accept order event " + event.eventId());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for order event confirmation", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("No broker confirmation for order event " + event.eventId(), exception);
        }
    }
}
