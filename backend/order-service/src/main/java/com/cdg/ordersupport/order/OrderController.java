package com.cdg.ordersupport.order;

import com.cdg.ordersupport.common.CorrelationIdFilter;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orders;

    public OrderController(OrderService orders) { this.orders = orders; }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request,
            Authentication authentication,
            @RequestAttribute(CorrelationIdFilter.ATTRIBUTE) String correlationId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orders.create(request, authentication, correlationId));
    }

    @GetMapping
    public OrderPage list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, Authentication authentication) {
        return orders.list(page, size, authentication);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id, Authentication authentication) {
        return orders.get(id, authentication);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public OrderResponse cancel(@PathVariable UUID id, Authentication authentication,
            @RequestAttribute(CorrelationIdFilter.ATTRIBUTE) String correlationId) {
        return orders.cancel(id, authentication, correlationId);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponse updateStatus(@PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            @RequestAttribute(CorrelationIdFilter.ATTRIBUTE) String correlationId) {
        return orders.updateStatus(id, request.status(), correlationId);
    }
}
