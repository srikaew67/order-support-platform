package com.cdg.ordersupport.order;

import com.cdg.ordersupport.common.ApiException;
import com.cdg.ordersupport.product.Product;
import com.cdg.ordersupport.product.ProductCache;
import com.cdg.ordersupport.product.ProductRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final ProductRepository products;
    private final ProductCache cache;
    private final OrderEventPublisher events;

    public OrderService(OrderRepository orders, ProductRepository products, ProductCache cache,
            OrderEventPublisher events) {
        this.orders = orders;
        this.products = products;
        this.cache = cache;
        this.events = events;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request, Authentication authentication, String correlationId) {
        UUID customerId = UUID.fromString(authentication.getName());
        CustomerOrder order = new CustomerOrder(customerId);
        var items = request.items().stream()
                .sorted(Comparator.comparing(item -> item.productId().toString())).toList();
        var seen = new HashSet<UUID>();
        for (CreateOrderRequest.Item item : items) {
            if (!seen.add(item.productId())) throw new ApiException(HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR", "Each product may appear only once in an order");
            Product product = products.findActiveForUpdate(item.productId()).orElseThrow(() ->
                    new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Product not found"));
            product.reserve(item.quantity());
            order.addItem(product.getId(), item.quantity(), product.getPrice());
        }
        if (order.getTotalAmount().compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Order total is too large");
        }
        CustomerOrder saved = orders.save(order);
        afterCommit(OrderEvent.created(saved, correlationId), true);
        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public OrderPage list(int page, int size, Authentication authentication) {
        if (page < 0 || size < 1 || size > 100) throw new ApiException(HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR", "Page must be nonnegative and size must be between 1 and 100");
        PageRequest paging = PageRequest.of(page, size,
                Sort.by("createdAt").descending().and(Sort.by("id").descending()));
        Page<CustomerOrder> found = canViewAll(authentication)
                ? orders.findAll(paging)
                : orders.findByCustomerId(UUID.fromString(authentication.getName()), paging);
        return new OrderPage(found.getContent().stream().map(OrderResponse::from).toList(),
                page, size, found.getTotalElements(), found.getTotalPages());
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID id, Authentication authentication) {
        CustomerOrder order = existing(id);
        requireViewer(order, authentication);
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cancel(UUID id, Authentication authentication, String correlationId) {
        CustomerOrder order = locked(id);
        requireOwnerOrAdmin(order, authentication);
        if (order.getStatus() == OrderStatus.CANCELLED) return OrderResponse.from(order);
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PROCESSING) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_TRANSITION", "Order can no longer be cancelled");
        }
        for (OrderItem item : order.getItems().stream()
                .sorted(Comparator.comparing(i -> i.getProductId().toString())).toList()) {
            Product product = products.findForUpdate(item.getProductId()).orElseThrow();
            product.restore(item.getQuantity());
        }
        order.setStatus(OrderStatus.CANCELLED);
        afterCommit(OrderEvent.statusChanged(order, correlationId), true);
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse updateStatus(UUID id, OrderStatus next, String correlationId) {
        CustomerOrder order = locked(id);
        boolean allowed = switch (order.getStatus()) {
            case PENDING -> next == OrderStatus.PROCESSING;
            case PROCESSING -> next == OrderStatus.SHIPPED;
            case SHIPPED -> next == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
        if (!allowed) throw new ApiException(HttpStatus.CONFLICT,
                "INVALID_TRANSITION", "Invalid order status transition");
        order.setStatus(next);
        afterCommit(OrderEvent.statusChanged(order, correlationId), false);
        return OrderResponse.from(order);
    }

    private CustomerOrder existing(UUID id) {
        return orders.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                "NOT_FOUND", "Order not found"));
    }

    private CustomerOrder locked(UUID id) {
        return orders.findForUpdate(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                "NOT_FOUND", "Order not found"));
    }

    private boolean canViewAll(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(authority ->
                authority.getAuthority().equals("ROLE_ADMIN") || authority.getAuthority().equals("ROLE_SUPPORT"));
    }

    private void requireViewer(CustomerOrder order, Authentication authentication) {
        if (!canViewAll(authentication) && !order.getCustomerId().toString().equals(authentication.getName())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Order belongs to another customer");
        }
    }

    private void requireOwnerOrAdmin(CustomerOrder order, Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream().anyMatch(authority ->
                authority.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && !order.getCustomerId().toString().equals(authentication.getName())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Order belongs to another customer");
        }
    }

    private void afterCommit(OrderEvent event, boolean stockChanged) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                if (stockChanged) {
                    try { cache.invalidateAll(); } catch (RuntimeException ignored) { }
                }
                events.publish(event);
            }
        });
    }
}
