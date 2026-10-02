package com.cdg.ordersupport.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.cdg.ordersupport.common.ApiException;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "products")
public class Product {
    @Id private UUID id;
    @Column(nullable = false, unique = true) private String sku;
    @Column(nullable = false) private String name;
    private String description;
    @Column(nullable = false) private BigDecimal price;
    @Column(name = "stock_quantity", nullable = false) private int stockQuantity;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version @Column(nullable = false) private long version;

    protected Product() {}

    public Product(String sku, String name, String description, BigDecimal price, int stockQuantity) {
        this.id = UUID.randomUUID();
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public void update(String sku, String name, String description, BigDecimal price, int stockQuantity) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public void reserve(int quantity) {
        if (quantity > stockQuantity) {
            throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", "Not enough stock for product " + sku);
        }
        stockQuantity -= quantity;
        updatedAt = Instant.now();
    }

    public void restore(int quantity) {
        stockQuantity += quantity;
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public int getStockQuantity() { return stockQuantity; }
    public boolean isActive() { return active; }
    public long getVersion() { return version; }
}
