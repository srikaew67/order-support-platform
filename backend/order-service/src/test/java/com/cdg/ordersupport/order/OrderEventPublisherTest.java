package com.cdg.ordersupport.order;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
        new OrderEventPublisher(rabbit).publish(created);
        ArgumentCaptor<MessagePostProcessor> processor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbit).convertAndSend(eq("order.events"), eq("order.created"), eq(created), processor.capture());
        Message message = processor.getValue().postProcessMessage(new Message(new byte[0], new MessageProperties()));
        assertEquals(created.eventId().toString(), message.getMessageProperties().getMessageId());
        assertEquals("request-123", message.getMessageProperties().getHeader("X-Correlation-ID"));
    }
}
