package com.cdg.ordersupport.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank @Size(max = 80) String sku,
        @NotBlank @Size(max = 255) String name,
        String description,
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal price,
        @PositiveOrZero int stockQuantity) {}
