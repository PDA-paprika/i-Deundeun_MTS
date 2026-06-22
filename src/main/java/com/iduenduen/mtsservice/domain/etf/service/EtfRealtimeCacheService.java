package com.iduenduen.mtsservice.domain.etf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtfRealtimeCacheService {

    private static final String PRICE_KEY_PREFIX = "etf:price:latest:";
    private static final String ORDERBOOK_KEY_PREFIX = "etf:orderbook:latest:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public void cachePrice(String code, long price, long change, double changeRate, long volume) {
        PriceSnapshot snapshot = new PriceSnapshot(price, change, changeRate, volume, LocalDateTime.now());
        write(PRICE_KEY_PREFIX + code, snapshot, "price", code);
    }

    public Optional<PriceSnapshot> getCachedPrice(String code) {
        return read(PRICE_KEY_PREFIX + code, PriceSnapshot.class, "price", code);
    }

    public void cacheOrderBook(String code, long[] askPrices, long[] askQtys, long[] bidPrices, long[] bidQtys) {
        OrderBookSnapshotCache snapshot = new OrderBookSnapshotCache(askPrices, askQtys, bidPrices, bidQtys, LocalDateTime.now());
        write(ORDERBOOK_KEY_PREFIX + code, snapshot, "orderbook", code);
    }

    public Optional<OrderBookSnapshotCache> getCachedOrderBook(String code) {
        return read(ORDERBOOK_KEY_PREFIX + code, OrderBookSnapshotCache.class, "orderbook", code);
    }

    private void write(String key, Object value, String kind, String code) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value));
        } catch (Exception e) {
            log.warn("Failed to cache realtime {}. code={}", kind, code, e);
        }
    }

    private <T> Optional<T> read(String key, Class<T> type, String kind, String code) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, type));
        } catch (Exception e) {
            log.warn("Failed to parse cached {}. code={}", kind, code, e);
            return Optional.empty();
        }
    }

    public record PriceSnapshot(long price, long change, double changeRate, long volume, LocalDateTime updatedAt) {
    }

    public record OrderBookSnapshotCache(long[] askPrices, long[] askQtys, long[] bidPrices, long[] bidQtys, LocalDateTime updatedAt) {
    }
}
