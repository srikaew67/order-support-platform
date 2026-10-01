package com.cdg.ordersupport.product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(UUID id, String sku, String name, String description,
        BigDecimal price, int stockQuantity) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getSku(), product.getName(),
                product.getDescription(), product.getPrice(), product.getStockQuantity());
    }
}
