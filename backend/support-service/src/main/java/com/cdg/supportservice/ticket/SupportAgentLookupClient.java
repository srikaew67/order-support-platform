package com.cdg.supportservice.ticket;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class SupportAgentLookupClient {
    private final RestTemplate http;
    private final String baseUrl;
    public SupportAgentLookupClient(RestTemplateBuilder builder, @Value("${services.order.url}") String baseUrl,
            @Value("${services.order.connect-timeout:2s}") Duration connectTimeout,
            @Value("${services.order.read-timeout:3s}") Duration readTimeout) {
        this.http = builder.setConnectTimeout(connectTimeout).setReadTimeout(readTimeout).build();
        this.baseUrl = baseUrl;
    }
    public void validate(UUID agentId, String authorization) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        try {
            var response = http.exchange(baseUrl + "/api/v1/support-agents/" + agentId,
                    HttpMethod.GET, new HttpEntity<>(headers), SupportAgent.class);
            if (response.getBody() == null || !agentId.equals(response.getBody().id()))
                throw invalidAssignee();
        } catch (HttpClientErrorException.NotFound exception) {
            throw invalidAssignee();
        } catch (RestClientException | CancellationException exception) {
            throw new TicketException(HttpStatus.BAD_GATEWAY, "IDENTITY_SERVICE_UNAVAILABLE",
                    "Unable to validate support agent");
        }
    }
    private static TicketException invalidAssignee() {
        return new TicketException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_ASSIGNEE", "Support agent not found");
    }
    private record SupportAgent(UUID id) {}
}
