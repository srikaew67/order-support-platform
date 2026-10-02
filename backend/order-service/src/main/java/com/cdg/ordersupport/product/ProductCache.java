package com.cdg.ordersupport.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductCache {
    private static final Duration TTL = Duration.ofMinutes(2);
    private final StringRedisTemplate redis;
    private final ObjectMapper json;

    public ProductCache(StringRedisTemplate redis, ObjectMapper json) {
        this.redis = redis;
        this.json = json;
    }

    public Optional<ProductPage> readPage(int page, int size) {
        return read("products:v2:page:" + page + ":" + size, ProductPage.class);
    }

    public void writePage(int page, int size, ProductPage value) {
        write("products:v2:page:" + page + ":" + size, value);
    }

    public Optional<ProductResponse> readProduct(UUID id) {
        return read("products:v2:item:" + id, ProductResponse.class);
    }

    public void writeProduct(UUID id, ProductResponse value) {
        write("products:v2:item:" + id, value);
    }

    public void invalidateAll() {
        Set<String> keys = redis.keys("products:*");
        if (keys != null && !keys.isEmpty()) redis.delete(keys);
    }

    private <T> Optional<T> read(String key, Class<T> type) {
        String data = redis.opsForValue().get(key);
        if (data == null) return Optional.empty();
        try {
            return Optional.of(json.readValue(data, type));
        } catch (JsonProcessingException exception) {
            redis.delete(key);
            return Optional.empty();
        }
    }

    private void write(String key, Object value) {
        try {
            redis.opsForValue().set(key, json.writeValueAsString(value), TTL);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize product cache value", exception);
        }
    }
}
