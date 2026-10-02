package com.cdg.ordersupport.order;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

class OrderEventPublisherTest {
    @Test void eventHasUniqueIdVersionedJsonAndCorrelationHeader() throws Exception {
        CustomerOrder order = new CustomerOrder(UUID.randomUUID());
        OrderEvent created = OrderEvent.created(order, "request-123");
        OrderEvent changed = OrderEvent.statusChanged(order, "request-123");
        assertNotEquals(created.eventId(), changed.eventId());
        assertEquals(1, created.version());
        ObjectMapper json = Jackson2ObjectMapperBuilder.json().build();
        String payload = json.writeValueAsString(created);
        assertTrue(payload.contains("\"event_id\""));
        assertTrue(payload.contains("\"event_type\":\"OrderCreated\""));
        assertTrue(payload.contains("\"version\":1"));

        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        doAnswer(call -> {
            CorrelationData confirmation = call.getArgument(4);
            confirmation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).convertAndSend(eq("order.events"), eq("order.created"), eq(created),
                any(MessagePostProcessor.class), any(CorrelationData.class));
        new OrderEventPublisher(rabbit).publish(created);
        ArgumentCaptor<MessagePostProcessor> processor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbit).convertAndSend(eq("order.events"), eq("order.created"), eq(created),
                processor.capture(), any(CorrelationData.class));
        Message message = processor.getValue().postProcessMessage(new Message(new byte[0], new MessageProperties()));
        assertEquals(created.eventId().toString(), message.getMessageProperties().getMessageId());
        assertEquals("request-123", message.getMessageProperties().getHeader("X-Correlation-ID"));
    }

    @Test void brokerNackKeepsEventUnconfirmed() {
        OrderEvent event = OrderEvent.created(new CustomerOrder(UUID.randomUUID()), "request-123");
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        doAnswer(call -> {
            CorrelationData confirmation = call.getArgument(4);
            confirmation.getFuture().complete(new CorrelationData.Confirm(false, "broker nack"));
            return null;
        }).when(rabbit).convertAndSend(eq("order.events"), eq("order.created"), eq(event),
                any(MessagePostProcessor.class), any(CorrelationData.class));
        assertThrows(IllegalStateException.class, () -> new OrderEventPublisher(rabbit).publish(event));
    }

    @Test void unroutableEventIsNotConsideredDelivered() {
        OrderEvent event = OrderEvent.created(new CustomerOrder(UUID.randomUUID()), "request-123");
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        doAnswer(call -> {
            CorrelationData confirmation = call.getArgument(4);
            confirmation.setReturned(new ReturnedMessage(
                    new Message(new byte[0], new MessageProperties()), 312, "NO_ROUTE", "order.events", "order.created"));
            confirmation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).convertAndSend(eq("order.events"), eq("order.created"), eq(event),
                any(MessagePostProcessor.class), any(CorrelationData.class));
        assertThrows(IllegalStateException.class, () -> new OrderEventPublisher(rabbit).publish(event));
    }
}
