package com.cdg.notificationservice.notification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.retry.support.RetryTemplate;

class NotificationListenerTest {
    private final Message message = new Message("{}".getBytes(), new MessageProperties());
    @Test void permanentMalformedEventIsRejectedWithoutRetry() {
        NotificationEventParser parser = mock(NotificationEventParser.class);
        NotificationProcessor processor = mock(NotificationProcessor.class);
        when(parser.parse(message)).thenThrow(new InvalidNotificationEventException("bad payload"));
        var listener = new NotificationListener(parser, processor, RetryTemplate.builder().maxAttempts(3).fixedBackoff(1).build());
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.handle(message));
        verify(parser, times(1)).parse(message);
        verifyNoInteractions(processor);
    }
    @Test void transientFailureCanRecoverBeforeRetryLimit() {
        NotificationEventParser parser = mock(NotificationEventParser.class);
        NotificationProcessor processor = mock(NotificationProcessor.class);
        NotificationEvent event = new NotificationEvent(UUID.randomUUID(), "OrderStatusChanged", UUID.randomUUID(),
                UUID.randomUUID(), "SHIPPED", "request-2");
        when(parser.parse(message)).thenReturn(event);
        doThrow(new IllegalStateException("temporary failure"))
                .doNothing().when(processor).persist(event);
        var listener = new NotificationListener(parser, processor,
                RetryTemplate.builder().maxAttempts(3).fixedBackoff(1).build());
        assertDoesNotThrow(() -> listener.handle(message));
        verify(processor, times(2)).persist(event);
    }
    @Test void transientPersistenceFailureRetriesThreeTimesThenDeadLetters() {
        NotificationEventParser parser = mock(NotificationEventParser.class);
        NotificationProcessor processor = mock(NotificationProcessor.class);
        NotificationEvent event = new NotificationEvent(UUID.randomUUID(), "OrderCreated", UUID.randomUUID(),
                UUID.randomUUID(), "PENDING", "request-1");
        when(parser.parse(message)).thenReturn(event);
        doThrow(new IllegalStateException("database unavailable")).when(processor).persist(event);
        var listener = new NotificationListener(parser, processor, RetryTemplate.builder().maxAttempts(3).fixedBackoff(1).build());
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.handle(message));
        verify(processor, times(3)).persist(event);
    }
}
