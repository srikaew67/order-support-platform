package com.cdg.ordersupport.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(@NotEmpty @Size(max = 50) List<@Valid Item> items) {
    public record Item(@NotNull UUID productId, @Min(1) int quantity) {}
}
