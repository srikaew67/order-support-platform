package com.cdg.supportservice.ticket;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class OrderLookupClient {
    private final RestTemplate http;
    private final String baseUrl;
    public OrderLookupClient(RestTemplateBuilder builder, @Value("${services.order.url}") String baseUrl) {
        this.http = builder.build(); this.baseUrl = baseUrl;
    }
    public void validate(UUID orderId, UUID customerId, String authorization) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        try {
            ResponseEntity<OrderReference> response = http.exchange(baseUrl + "/api/v1/orders/" + orderId,
                    HttpMethod.GET, new HttpEntity<>(headers), OrderReference.class);
            if (response.getBody() == null || !customerId.equals(response.getBody().customerId()))
                throw new TicketException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Order belongs to another customer");
        } catch (HttpClientErrorException.Forbidden exception) {
            throw new TicketException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Order belongs to another customer");
        } catch (HttpClientErrorException.NotFound exception) {
            throw new TicketException(HttpStatus.BAD_REQUEST, "INVALID_ORDER", "Order not found");
        } catch (RestClientException exception) {
            throw new TicketException(HttpStatus.BAD_GATEWAY, "ORDER_SERVICE_UNAVAILABLE", "Unable to validate order");
        }
    }
    private record OrderReference(UUID customerId) {}
}
