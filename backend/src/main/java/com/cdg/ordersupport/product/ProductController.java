package com.cdg.ordersupport.product;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/products")
public class ProductController {
  private final ProductRepository products;
  public ProductController(ProductRepository products){this.products=products;}
  @GetMapping public List<Product> list(){return products.findAll().stream().filter(Product::isActive).toList();}
}
