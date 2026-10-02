package com.cdg.supportservice.ticket;

import com.cdg.supportservice.common.CorrelationIdFilter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class TicketEventPublisher {
    private final RabbitTemplate rabbit;
    public TicketEventPublisher(RabbitTemplate rabbit) { this.rabbit = rabbit; }
    public void publish(TicketEvent event) {
        CorrelationData confirmation = new CorrelationData(event.eventId().toString());
        rabbit.convertAndSend("ticket.events", "ticket.status.changed", event, message -> {
            message.getMessageProperties().setMessageId(event.eventId().toString());
            message.getMessageProperties().setHeader(CorrelationIdFilter.HEADER, event.correlationId());
            return message;
        }, confirmation);
        try {
            CorrelationData.Confirm result = confirmation.getFuture().get(5, TimeUnit.SECONDS);
            if (!result.isAck() || confirmation.getReturned() != null)
                throw new IllegalStateException("Broker did not accept ticket event " + event.eventId());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for ticket event confirmation", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("No broker confirmation for ticket event " + event.eventId(), exception);
        }
    }
}
