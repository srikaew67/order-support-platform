package com.cdg.ordersupport.product;
import jakarta.validation.constraints.*; import java.math.BigDecimal;
public record CreateProductRequest(@NotBlank String sku,@NotBlank String name,String description,@NotNull @Positive BigDecimal price,@PositiveOrZero int stockQuantity) {}
