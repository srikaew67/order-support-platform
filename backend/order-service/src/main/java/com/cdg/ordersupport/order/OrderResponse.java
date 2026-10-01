package com.cdg.ordersupport.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(UUID id, String orderNumber, UUID customerId, OrderStatus status,
        BigDecimal totalAmount, List<Item> items, Instant createdAt) {
    public record Item(UUID productId, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {}

    public static OrderResponse from(CustomerOrder order) {
        return new OrderResponse(order.getId(), order.getOrderNumber(), order.getCustomerId(),
                order.getStatus(), order.getTotalAmount(), order.getItems().stream()
                        .map(item -> new Item(item.getProductId(), item.getQuantity(),
                                item.getUnitPrice(), item.getSubtotal())).toList(), order.getCreatedAt());
    }
}
