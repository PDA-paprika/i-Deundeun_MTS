package com.iduenduen.mtsservice.domain.etf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtfRealtimeCacheService {

    private static final String PRICE_KEY_PREFIX = "etf:price:latest:";
    private static final String ORDERBOOK_KEY_PREFIX = "etf:orderbook:latest:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public void cachePrice(String code, long price, long change, double changeRate, long volume, long tradeAmount) {
        PriceSnapshot snapshot = new PriceSnapshot(price, change, changeRate, volume, tradeAmount, LocalDateTime.now());
        write(PRICE_KEY_PREFIX + code, snapshot, "price", code);
    }

    public Optional<PriceSnapshot> getCachedPrice(String code) {
        return read(PRICE_KEY_PREFIX + code, PriceSnapshot.class, "price", code);
    }

    // 여러 종목 시세 캐시를 MGET 한 번으로 읽는다. (리스트 랭킹의 종목별 개별 GET N번을 1번으로 줄임)
    public Map<String, PriceSnapshot> getCachedPrices(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Map.of();
        }
        List<String> codeList = new ArrayList<>(codes);
        List<String> keys = codeList.stream().map(c -> PRICE_KEY_PREFIX + c).toList();
        List<String> values = redisTemplate.opsForValue().multiGet(keys);
        Map<String, PriceSnapshot> result = new HashMap<>();
        if (values == null) {
            return result;
        }
        for (int i = 0; i < codeList.size(); i++) {
            String json = values.get(i);
            if (json == null) {
                continue;
            }
            try {
                result.put(codeList.get(i), objectMapper.readValue(json, PriceSnapshot.class));
            } catch (Exception e) {
                log.warn("Failed to parse cached price. code={}", codeList.get(i), e);
            }
        }
        return result;
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

    public record PriceSnapshot(long price, long change, double changeRate, long volume, long tradeAmount, LocalDateTime updatedAt) {
    }

    public record OrderBookSnapshotCache(long[] askPrices, long[] askQtys, long[] bidPrices, long[] bidQtys, LocalDateTime updatedAt) {
    }
}
