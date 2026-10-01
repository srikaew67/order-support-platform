package com.cdg.ordersupport.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderEvent(
        @JsonProperty("event_id") UUID eventId,
        @JsonProperty("event_type") String eventType,
        int version,
        @JsonProperty("occurred_at") Instant occurredAt,
        @JsonProperty("order_id") UUID orderId,
        @JsonProperty("customer_id") UUID customerId,
        String status,
        @JsonProperty("total_amount") BigDecimal totalAmount,
        @JsonProperty("correlation_id") String correlationId) {
    public static OrderEvent created(CustomerOrder order, String correlationId) {
        return of("OrderCreated", order, correlationId);
    }

    public static OrderEvent statusChanged(CustomerOrder order, String correlationId) {
        return of("OrderStatusChanged", order, correlationId);
    }

    private static OrderEvent of(String eventType, CustomerOrder order, String correlationId) {
        return new OrderEvent(UUID.randomUUID(), eventType, 1, Instant.now(), order.getId(),
                order.getCustomerId(), order.getStatus().name(), order.getTotalAmount(), correlationId);
    }
}
