package com.cdg.ordersupport.product;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*; import org.springframework.security.access.prepost.PreAuthorize; import jakarta.validation.Valid;
@RestController @RequestMapping("/api/v1/products")
public class ProductController {
  private final ProductRepository products;
  public ProductController(ProductRepository products){this.products=products;}
  @GetMapping public List<Product> list(){return products.findAll().stream().filter(Product::isActive).toList();}
  @PostMapping @PreAuthorize("hasRole('ADMIN')") ResponseEntity<Product> create(@Valid @RequestBody CreateProductRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(products.save(new Product(request.sku(),request.name(),request.description(),request.price(),request.stockQuantity())));}
}
