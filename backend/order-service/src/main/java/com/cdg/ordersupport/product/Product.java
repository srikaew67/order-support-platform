package com.cdg.ordersupport.product;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "products")
public class Product {
  @Id private UUID id;
  @Column(nullable=false, unique=true) private String sku;
  @Column(nullable=false) private String name;
  private String description;
  @Column(nullable=false) private BigDecimal price;
  @Column(name="stock_quantity", nullable=false) private int stockQuantity;
  @Column(nullable=false) private boolean active;
  @Column(name="created_at", nullable=false) private Instant createdAt;
  @Column(name="updated_at", nullable=false) private Instant updatedAt;
  protected Product() {}
  public Product(String sku,String name,String description,BigDecimal price,int stockQuantity){this.id=UUID.randomUUID();this.sku=sku;this.name=name;this.description=description;this.price=price;this.stockQuantity=stockQuantity;this.active=true;this.createdAt=Instant.now();this.updatedAt=this.createdAt;}
  public UUID getId(){return id;} public String getSku(){return sku;} public String getName(){return name;} public BigDecimal getPrice(){return price;} public int getStockQuantity(){return stockQuantity;} public boolean isActive(){return active;}
}
