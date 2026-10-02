package com.cdg.notificationservice.notification;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class NotificationMessagingConfig {
    public static final String EVENT_QUEUE = "notifications.events";
    public static final String DEAD_QUEUE = "notifications.dead";
    @Bean TopicExchange orderEventsExchange() { return new TopicExchange("order.events", true, false); }
    @Bean DirectExchange ticketEventsExchange() { return new DirectExchange("ticket.events", true, false); }
    @Bean DirectExchange deadLetterExchange() { return new DirectExchange("notifications.dlx", true, false); }
    @Bean Queue notificationQueue() {
        return QueueBuilder.durable(EVENT_QUEUE).deadLetterExchange("notifications.dlx")
                .deadLetterRoutingKey("notification.dead").build();
    }
    @Bean Queue deadLetterQueue() { return QueueBuilder.durable(DEAD_QUEUE).build(); }
    @Bean Binding orderCreatedBinding(Queue notificationQueue, TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(notificationQueue).to(orderEventsExchange).with("order.created");
    }
    @Bean Binding orderStatusBinding(Queue notificationQueue, TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(notificationQueue).to(orderEventsExchange).with("order.status.changed");
    }
    @Bean Binding ticketStatusBinding(Queue notificationQueue, DirectExchange ticketEventsExchange) {
        return BindingBuilder.bind(notificationQueue).to(ticketEventsExchange).with("ticket.status.changed");
    }
    @Bean Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with("notification.dead");
    }
    @Bean RetryTemplate notificationRetryTemplate() {
        return RetryTemplate.builder().maxAttempts(3).fixedBackoff(200).build();
    }
}
