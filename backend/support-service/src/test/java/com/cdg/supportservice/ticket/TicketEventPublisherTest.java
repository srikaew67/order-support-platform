package com.cdg.supportservice.ticket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cdg.supportservice.common.CorrelationIdFilter;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;

class TicketEventPublisherTest {
    @Test void sendsJsonEventWithStableIdAndBrokerConfirmation() {
        MessageConverter converter = new TicketMessagingConfig().ticketEventMessageConverter(new ObjectMapper().findAndRegisterModules());
        assertInstanceOf(Jackson2JsonMessageConverter.class, converter);
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        TicketEvent event = new TicketEvent(UUID.randomUUID(), "TicketStatusChanged", 1, Instant.now(),
                UUID.randomUUID(), UUID.randomUUID(), "IN_PROGRESS", "request-123");
        doAnswer(call -> {
            MessagePostProcessor processor = call.getArgument(3);
            CorrelationData data = call.getArgument(4);
            Message message = processor.postProcessMessage(converter.toMessage(event, new org.springframework.amqp.core.MessageProperties()));
            assertEquals(event.eventId().toString(), message.getMessageProperties().getMessageId());
            assertEquals("request-123", message.getMessageProperties().getHeaders().get(CorrelationIdFilter.HEADER));
            assertEquals(event.eventId().toString(), data.getId());
            data.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).convertAndSend(eq("ticket.events"), eq("ticket.status.changed"), eq(event),
                any(MessagePostProcessor.class), any(CorrelationData.class));
        assertDoesNotThrow(() -> new TicketEventPublisher(rabbit).publish(event));
    }
    @Test void rejectsBrokerNack() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        TicketEvent event = new TicketEvent(UUID.randomUUID(), "TicketStatusChanged", 1, Instant.now(),
                UUID.randomUUID(), UUID.randomUUID(), "RESOLVED", "request-456");
        doAnswer(call -> {
            CorrelationData data = call.getArgument(4);
            data.getFuture().complete(new CorrelationData.Confirm(false, "broker down"));
            return null;
        }).when(rabbit).convertAndSend(anyString(), anyString(), any(TicketEvent.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));
        assertThrows(IllegalStateException.class, () -> new TicketEventPublisher(rabbit).publish(event));
    }
}
