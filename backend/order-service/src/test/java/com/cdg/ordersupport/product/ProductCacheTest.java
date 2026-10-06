package com.cdg.ordersupport.product;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

class ProductCacheTest {
    @Test void cachedPageAndDetailAreRemovedAfterInvalidation() {
        Map<String, String> values = new HashMap<>();
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(operations);
        when(operations.get(anyString())).thenAnswer(call -> values.get(call.getArgument(0)));
        doAnswer(call -> {
            values.put(call.getArgument(0), call.getArgument(1));
            return null;
        }).when(operations).set(anyString(), anyString(), any(Duration.class));
        var scanned = new java.util.ArrayList<String>();
        @SuppressWarnings("unchecked") Cursor<String> cursor = mock(Cursor.class);
        when(redis.scan(any(ScanOptions.class))).thenAnswer(call -> {
            scanned.clear();
            scanned.addAll(values.keySet());
            return cursor;
        });
        when(cursor.hasNext()).thenAnswer(call -> !scanned.isEmpty());
        when(cursor.next()).thenAnswer(call -> scanned.removeFirst());
        when(redis.delete(anyCollection())).thenAnswer(call -> {
            var keys = (java.util.Collection<?>) call.getArgument(0);
            keys.forEach(values::remove);
            return (long) keys.size();
        });
        ObjectMapper json = Jackson2ObjectMapperBuilder.json().build();
        ProductCache cache = new ProductCache(redis, json);
        UUID id = UUID.randomUUID();
        ProductResponse product = new ProductResponse(id, "SKU", "Desk", "Oak", BigDecimal.TEN, 2, 0);
        ProductPage page = new ProductPage(List.of(product), 0, 12, 1, 1);

        cache.writePage(0, 12, page);
        cache.writeProduct(id, product);
        assertEquals("Desk", cache.readPage(0, 12).orElseThrow().content().getFirst().name());
        assertEquals("Oak", cache.readProduct(id).orElseThrow().description());
        cache.invalidateAll();
        verify(redis).scan(any(ScanOptions.class));
        verify(redis, never()).keys(anyString());
        assertTrue(cache.readPage(0, 12).isEmpty());
        assertTrue(cache.readProduct(id).isEmpty());
    }
}
