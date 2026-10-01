package com.cdg.ordersupport.product;

import com.cdg.ordersupport.common.ApiException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ProductService {
    private final ProductRepository products;
    private final ProductCache cache;

    public ProductService(ProductRepository products, ProductCache cache) {
        this.products = products;
        this.cache = cache;
    }

    @Transactional(readOnly = true)
    public ProductPage list(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new ApiException(HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR", "Page must be nonnegative and size must be between 1 and 100");
        try {
            var cached = cache.readPage(page, size);
            if (cached.isPresent()) return cached.get();
        } catch (RuntimeException ignored) { // Cache outages must not block catalog reads.
        }
        Page<Product> found = products.findByActiveTrue(PageRequest.of(page, size,
                Sort.by("name").ascending().and(Sort.by("id").ascending())));
        ProductPage result = new ProductPage(found.getContent().stream().map(ProductResponse::from).toList(),
                page, size, found.getTotalElements(), found.getTotalPages());
        try { cache.writePage(page, size, result); } catch (RuntimeException ignored) { }
        return result;
    }

    @Transactional(readOnly = true)
    public ProductResponse get(UUID id) {
        try {
            var cached = cache.readProduct(id);
            if (cached.isPresent()) return cached.get();
        } catch (RuntimeException ignored) { }
        ProductResponse result = ProductResponse.from(activeProduct(id));
        try { cache.writeProduct(id, result); } catch (RuntimeException ignored) { }
        return result;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        if (products.existsBySku(request.sku())) throw conflict();
        ProductResponse result = ProductResponse.from(products.save(new Product(request.sku(), request.name(),
                request.description(), request.price(), request.stockQuantity())));
        invalidateAfterCommit();
        return result;
    }

    @Transactional
    public ProductResponse update(UUID id, CreateProductRequest request) {
        Product product = activeProduct(id);
        if (products.existsBySkuAndIdNot(request.sku(), id)) throw conflict();
        product.update(request.sku(), request.name(), request.description(), request.price(), request.stockQuantity());
        ProductResponse result = ProductResponse.from(products.save(product));
        invalidateAfterCommit();
        return result;
    }

    @Transactional
    public void delete(UUID id) {
        Product product = activeProduct(id);
        product.deactivate();
        products.save(product);
        invalidateAfterCommit();
    }

    private Product activeProduct(UUID id) {
        return products.findByIdAndActiveTrue(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                "NOT_FOUND", "Product not found"));
    }

    private ApiException conflict() {
        return new ApiException(HttpStatus.CONFLICT, "CONFLICT", "SKU already exists");
    }

    private void invalidateAfterCommit() {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { invalidate(); }
        });
    }

    private void invalidate() {
        try { cache.invalidateAll(); } catch (RuntimeException ignored) { }
    }
}
