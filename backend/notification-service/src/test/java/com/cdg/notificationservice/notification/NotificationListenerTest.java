package com.cdg.notificationservice.notification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.dao.DataIntegrityViolationException;
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
    @Test void duplicateKeyCollisionIsAcceptedWhenEventIsAlreadyStored() {
        NotificationEventParser parser = mock(NotificationEventParser.class);
        NotificationProcessor processor = mock(NotificationProcessor.class);
        NotificationEvent event = new NotificationEvent(UUID.randomUUID(), "OrderCreated", UUID.randomUUID(),
                UUID.randomUUID(), "PENDING", "request-duplicate");
        when(parser.parse(message)).thenReturn(event);
        doThrow(new DataIntegrityViolationException("event_id unique key"))
                .when(processor).persist(event);
        when(processor.alreadyProcessed(event.eventId())).thenReturn(true);
        var listener = new NotificationListener(parser, processor,
                RetryTemplate.builder().maxAttempts(3).fixedBackoff(1).build());
        assertDoesNotThrow(() -> listener.handle(message));
        verify(processor, times(1)).persist(event);
    }
    @Test void unrelatedIntegrityFailureStillRetriesAndDeadLetters() {
        NotificationEventParser parser = mock(NotificationEventParser.class);
        NotificationProcessor processor = mock(NotificationProcessor.class);
        NotificationEvent event = new NotificationEvent(UUID.randomUUID(), "OrderCreated", UUID.randomUUID(),
                UUID.randomUUID(), "PENDING", "request-integrity");
        when(parser.parse(message)).thenReturn(event);
        doThrow(new DataIntegrityViolationException("unrelated constraint"))
                .when(processor).persist(event);
        when(processor.alreadyProcessed(event.eventId())).thenReturn(false);
        var listener = new NotificationListener(parser, processor,
                RetryTemplate.builder().maxAttempts(3).fixedBackoff(1).build());
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.handle(message));
        verify(processor, times(3)).persist(event);
        verify(processor, times(3)).alreadyProcessed(event.eventId());
    }
    @Test void failureLogsIncludeCorrelationIdAndMdcIsCleared() {
        Logger logger = (Logger) LoggerFactory.getLogger(NotificationListener.class);
        ListAppender<ILoggingEvent> logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
        try {
            NotificationEventParser parser = mock(NotificationEventParser.class);
            NotificationProcessor processor = mock(NotificationProcessor.class);
            MessageProperties properties = new MessageProperties();
            properties.setHeader("X-Correlation-ID", "request-malformed");
            Message malformed = new Message("{}".getBytes(), properties);
            when(parser.parse(malformed)).thenThrow(new InvalidNotificationEventException("bad payload"));
            var listener = new NotificationListener(parser, processor,
                    RetryTemplate.builder().maxAttempts(2).fixedBackoff(1).build());
            assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.handle(malformed));
            assertTrue(logs.list.stream().anyMatch(log ->
                    log.getFormattedMessage().contains("request-malformed")));
            assertNull(MDC.get("correlationId"));

            logs.list.clear();
            NotificationEvent event = new NotificationEvent(UUID.randomUUID(), "OrderCreated", UUID.randomUUID(),
                    UUID.randomUUID(), "PENDING", "request-retry");
            when(parser.parse(message)).thenReturn(event);
            doThrow(new IllegalStateException("database unavailable")).when(processor).persist(event);
            assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.handle(message));
            assertEquals(3, logs.list.stream().filter(log ->
                    log.getFormattedMessage().contains("request-retry")).count());
            assertNull(MDC.get("correlationId"));
        } finally {
            logger.detachAppender(logs);
            logs.stop();
        }
    }
}
