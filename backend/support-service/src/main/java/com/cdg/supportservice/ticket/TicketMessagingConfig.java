package com.cdg.supportservice.ticket;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TicketMessagingConfig {
    @Bean MessageConverter ticketEventMessageConverter(ObjectMapper mapper) { return new Jackson2JsonMessageConverter(mapper); }
    @Bean DirectExchange ticketExchange() { return new DirectExchange("ticket.events", true, false); }
}
