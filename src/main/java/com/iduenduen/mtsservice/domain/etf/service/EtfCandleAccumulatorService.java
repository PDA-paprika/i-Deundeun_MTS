package com.iduenduen.mtsservice.domain.etf.service;

import com.iduenduen.mtsservice.domain.etf.entity.Etf;
import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle10m;
import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1d;
import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1m;
import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1mo;
import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1w;
import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle30m;
import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle60m;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle10mRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1dRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1mRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1moRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1wRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle30mRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle60mRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfRepository;
import com.iduenduen.mtsservice.domain.etf.realtime.EtfCandleWebSocketHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtfCandleAccumulatorService {

    private static final DateTimeFormatter BUCKET_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 0);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(15, 30);

    private static final String KEY_1M = "etf:candle:1m:";
    private static final String KEY_10M = "etf:candle:10m:";
    private static final String KEY_30M = "etf:candle:30m:";
    private static final String KEY_60M = "etf:candle:60m:";
    private static final String KEY_1D = "etf:candle:1d:";
    private static final String KEY_1W = "etf:candle:1w:";
    private static final String KEY_1MO = "etf:candle:1mo:";
    private static final String KEY_CUM_VOLUME = "etf:cumvolume:";

    private static final DefaultRedisScript<Void> ACCUMULATE_SCRIPT;
    static {
        ACCUMULATE_SCRIPT = new DefaultRedisScript<>();
        ACCUMULATE_SCRIPT.setLocation(new ClassPathResource("scripts/accumulate-candle.lua"));
        ACCUMULATE_SCRIPT.setResultType(Void.class);
    }

    private final StringRedisTemplate redisTemplate;
    private final EtfRepository etfRepository;
    private final EtfCandleWebSocketHandler etfCandleWebSocketHandler;
    private final EtfCandle1mRepository etfCandle1mRepository;
    private final EtfCandle10mRepository etfCandle10mRepository;
    private final EtfCandle30mRepository etfCandle30mRepository;
    private final EtfCandle60mRepository etfCandle60mRepository;
    private final EtfCandle1dRepository etfCandle1dRepository;
    private final EtfCandle1wRepository etfCandle1wRepository;
    private final EtfCandle1moRepository etfCandle1moRepository;

    public void accumulate(String code, long price, long cumulativeVolume) {
        LocalDateTime now = LocalDateTime.now(KST);
        LocalTime time = now.toLocalTime();
        if (time.isBefore(MARKET_OPEN) || time.isAfter(MARKET_CLOSE)) {
            return;
        }

        long delta = computeVolumeDelta(code, cumulativeVolume);
        if (delta < 0) {
            delta = 0;
        }

        String nowBucket = now.format(BUCKET_FORMAT);
        String weekBucket = now.toLocalDate().with(DayOfWeek.MONDAY).atStartOfDay().format(BUCKET_FORMAT);
        String monthBucket = now.toLocalDate().withDayOfMonth(1).atStartOfDay().format(BUCKET_FORMAT);

        String priceStr = String.valueOf(price);
        String volumeStr = String.valueOf(delta);

        accumulateKey(KEY_1M + code, priceStr, volumeStr, nowBucket);
        broadcastLiveCandle(code, "1m", KEY_1M, now.withSecond(0).withNano(0));

        accumulateKey(KEY_10M + code, priceStr, volumeStr, nowBucket);
        broadcastLiveCandle(code, "10m", KEY_10M, flooredNow(10));

        accumulateKey(KEY_30M + code, priceStr, volumeStr, nowBucket);
        broadcastLiveCandle(code, "30m", KEY_30M, flooredNow(30));

        accumulateKey(KEY_60M + code, priceStr, volumeStr, nowBucket);
        broadcastLiveCandle(code, "60m", KEY_60M, flooredNow(60));

        accumulateKey(KEY_1D + code, priceStr, volumeStr, nowBucket);
        broadcastLiveCandle(code, "1d", KEY_1D, LocalDate.now().atStartOfDay());

        // t8451은 진행 중인 주/달의 캔들을 "오늘" 날짜로 내려준다 (마감된 주/달은 마지막 거래일로 저장됨).
        // flushWeekly/Monthly도 같은 convention(now() at flush time)이라, 라이브 브로드캐스트도 동일하게 맞춘다.
        accumulateKey(KEY_1W + code, priceStr, volumeStr, weekBucket);
        broadcastLiveCandle(code, "1w", KEY_1W, LocalDate.now().atStartOfDay());

        accumulateKey(KEY_1MO + code, priceStr, volumeStr, monthBucket);
        broadcastLiveCandle(code, "1mo", KEY_1MO, LocalDate.now().atStartOfDay());
    }

    private void broadcastLiveCandle(String code, String interval, String keyPrefix, LocalDateTime candleTime) {
        try {
            Map<Object, Object> data = getCurrentCandle(code, keyPrefix);
            if (data.isEmpty() || !isValid(data)) {
                return;
            }
            etfCandleWebSocketHandler.broadcastCandleUpdate(
                    code, interval, candleTime.toEpochSecond(ZoneOffset.UTC),
                    parse(data.get("open")), parse(data.get("high")), parse(data.get("low")),
                    parse(data.get("close")), parse(data.get("volume"))
            );
        } catch (Exception e) {
            log.warn("Failed to broadcast live candle. code={}, interval={}", code, interval, e);
        }
    }

    private long computeVolumeDelta(String code, long cumulativeVolume) {
        String key = KEY_CUM_VOLUME + code;
        String previous = redisTemplate.opsForValue().get(key);
        redisTemplate.opsForValue().set(key, String.valueOf(cumulativeVolume));

        if (previous == null) {
            return 0;
        }
        long previousVolume = Long.parseLong(previous);
        if (cumulativeVolume < previousVolume) {
            return cumulativeVolume;
        }
        return cumulativeVolume - previousVolume;
    }

    private void accumulateKey(String key, String price, String volumeDelta, String bucketStart) {
        try {
            redisTemplate.execute(ACCUMULATE_SCRIPT, java.util.List.of(key), price, volumeDelta, bucketStart);
        } catch (Exception e) {
            log.warn("Failed to accumulate candle. key={}", key, e);
        }
    }

    public Map<Object, Object> getCurrentCandle(String code, String keyPrefix) {
        return redisTemplate.opsForHash().entries(keyPrefix + code);
    }

    public String key1m() { return KEY_1M; }
    public String key10m() { return KEY_10M; }
    public String key30m() { return KEY_30M; }
    public String key60m() { return KEY_60M; }
    public String key1d() { return KEY_1D; }

    @Transactional
    public void flush1m(String code) {
        flush(code, KEY_1M, (etfId, data, snapKey) ->
                upsert1m(etfId, data, bucketTimeFromData(data, 1)), "1분봉");
    }

    @Transactional
    public void flush10m(String code) {
        flush(code, KEY_10M, (etfId, data, snapKey) ->
                upsert10m(etfId, data, bucketTimeFromData(data, 10)), "10분봉");
    }

    @Transactional
    public void flush30m(String code) {
        flush(code, KEY_30M, (etfId, data, snapKey) ->
                upsert30m(etfId, data, bucketTimeFromData(data, 30)), "30분봉");
    }

    @Transactional
    public void flush60m(String code) {
        flush(code, KEY_60M, (etfId, data, snapKey) ->
                upsert60m(etfId, data, bucketTimeFromData(data, 60)), "60분봉");
    }

    @Transactional
    public void flushDaily(String code) {
        flush(code, KEY_1D, (etfId, data, snapKey) ->
                upsertDaily(etfId, data, parseStartTime(data).toLocalDate().atStartOfDay()), "일봉");
    }

    // 금요일(또는 그 주 마지막 영업일) 장마감에만 호출됨 - 그 주 월요일 날짜로 저장
    @Transactional
    public void flushWeekly(String code) {
        flush(code, KEY_1W, (etfId, data, snapKey) ->
                upsertWeekly(etfId, data,
                        parseStartTime(data).toLocalDate().with(DayOfWeek.MONDAY).atStartOfDay()), "주봉");
    }

    // 그 달의 마지막 영업일에만 호출됨 - 그 달 1일 날짜로 저장
    @Transactional
    public void flushMonthly(String code) {
        flush(code, KEY_1MO, (etfId, data, snapKey) ->
                upsertMonthly(etfId, data,
                        parseStartTime(data).toLocalDate().withDayOfMonth(1).atStartOfDay()), "월봉");
    }

    private void flush(String code, String keyPrefix, FlushAction action, String label) {
        String key = keyPrefix + code;
        String snapKey = key + ":snap";

        try {
            redisTemplate.rename(key, snapKey);
        } catch (Exception e) {
            return;
        }

        Map<Object, Object> data = redisTemplate.opsForHash().entries(snapKey);
        if (data.isEmpty() || !isValid(data)) {
            redisTemplate.delete(snapKey);
            return;
        }

        try {
            Etf etf = etfRepository.findByCode(code).orElse(null);
            if (etf == null) {
                log.warn("Skip flush for unknown ETF. code={}, label={}", code, label);
                return;
            }
            action.run(etf.getId(), data, snapKey);
        } catch (Exception e) {
            log.error("{} flush 실패. code={}", label, code, e);
        } finally {
            redisTemplate.delete(snapKey);
        }
    }

    private boolean isValid(Map<Object, Object> data) {
        return data.get("open") != null && data.get("high") != null
                && data.get("low") != null && data.get("close") != null && data.get("volume") != null;
    }

    private LocalDateTime flooredNow(int bucketMinutes) {
        LocalDateTime now = LocalDateTime.now();
        int floored = now.getMinute() - (now.getMinute() % bucketMinutes);
        return now.withMinute(floored).withSecond(0).withNano(0);
    }

    // Redis에 저장된 startTime(첫 틱 시각, KST "yyyyMMddHHmm")을 파싱한다.
    // flush 순간의 now()로 라벨을 매기면 서버 타임존/플러시 지연에 따라 시각이 틀어지므로,
    // 데이터가 실제로 속한 시각을 그대로 사용해 봉 시각이 어긋나지 않게 한다.
    private LocalDateTime parseStartTime(Map<Object, Object> data) {
        String startTime = (String) data.get("startTime");
        return (startTime != null)
                ? LocalDateTime.parse(startTime, BUCKET_FORMAT)
                : LocalDateTime.now(KST).withSecond(0).withNano(0);
    }

    // startTime을 bucketMinutes 단위로 내려 분봉 버킷 시작 시각을 만든다 (ex. 09:17 → 30분봉이면 09:00).
    private LocalDateTime bucketTimeFromData(Map<Object, Object> data, int bucketMinutes) {
        LocalDateTime raw = parseStartTime(data);
        int floored = (raw.getMinute() / bucketMinutes) * bucketMinutes;
        return raw.toLocalDate().atTime(raw.getHour(), floored);
    }

    private long parse(Object value) {
        return Long.parseLong((String) value);
    }

    private void upsert1m(Long etfId, Map<Object, Object> data, LocalDateTime candleTime) {
        long open = parse(data.get("open")), high = parse(data.get("high")), low = parse(data.get("low"));
        long close = parse(data.get("close")), volume = parse(data.get("volume"));
        etfCandle1mRepository.findByEtfIdAndCandleTime(etfId, candleTime).ifPresentOrElse(
                existing -> existing.updateSnapshot(open, high, low, close, volume, existing.getTradeAmount()),
                () -> etfCandle1mRepository.save(EtfCandle1m.of(etfId, open, high, low, close, volume, 0L, candleTime))
        );
    }

    private void upsert10m(Long etfId, Map<Object, Object> data, LocalDateTime candleTime) {
        long open = parse(data.get("open")), high = parse(data.get("high")), low = parse(data.get("low"));
        long close = parse(data.get("close")), volume = parse(data.get("volume"));
        etfCandle10mRepository.findByEtfIdAndCandleTime(etfId, candleTime).ifPresentOrElse(
                existing -> existing.updateSnapshot(open, high, low, close, volume, existing.getTradeAmount()),
                () -> etfCandle10mRepository.save(EtfCandle10m.of(etfId, open, high, low, close, volume, 0L, candleTime))
        );
    }

    private void upsert30m(Long etfId, Map<Object, Object> data, LocalDateTime candleTime) {
        long open = parse(data.get("open")), high = parse(data.get("high")), low = parse(data.get("low"));
        long close = parse(data.get("close")), volume = parse(data.get("volume"));
        etfCandle30mRepository.findByEtfIdAndCandleTime(etfId, candleTime).ifPresentOrElse(
                existing -> existing.updateSnapshot(open, high, low, close, volume, existing.getTradeAmount()),
                () -> etfCandle30mRepository.save(EtfCandle30m.of(etfId, open, high, low, close, volume, 0L, candleTime))
        );
    }

    private void upsert60m(Long etfId, Map<Object, Object> data, LocalDateTime candleTime) {
        long open = parse(data.get("open")), high = parse(data.get("high")), low = parse(data.get("low"));
        long close = parse(data.get("close")), volume = parse(data.get("volume"));
        etfCandle60mRepository.findByEtfIdAndCandleTime(etfId, candleTime).ifPresentOrElse(
                existing -> existing.updateSnapshot(open, high, low, close, volume, existing.getTradeAmount()),
                () -> etfCandle60mRepository.save(EtfCandle60m.of(etfId, open, high, low, close, volume, 0L, candleTime))
        );
    }

    private void upsertDaily(Long etfId, Map<Object, Object> data, LocalDateTime candleTime) {
        long open = parse(data.get("open")), high = parse(data.get("high")), low = parse(data.get("low"));
        long close = parse(data.get("close")), volume = parse(data.get("volume"));
        etfCandle1dRepository.findByEtfIdAndCandleTime(etfId, candleTime).ifPresentOrElse(
                existing -> existing.updateSnapshot(open, high, low, close, volume, existing.getTradeAmount()),
                () -> etfCandle1dRepository.save(EtfCandle1d.of(etfId, open, high, low, close, volume, 0L, candleTime))
        );
    }

    private void upsertWeekly(Long etfId, Map<Object, Object> data, LocalDateTime candleTime) {
        long open = parse(data.get("open")), high = parse(data.get("high")), low = parse(data.get("low"));
        long close = parse(data.get("close")), volume = parse(data.get("volume"));
        etfCandle1wRepository.findByEtfIdAndCandleTime(etfId, candleTime).ifPresentOrElse(
                existing -> existing.updateSnapshot(open, high, low, close, volume, existing.getTradeAmount()),
                () -> etfCandle1wRepository.save(EtfCandle1w.of(etfId, open, high, low, close, volume, 0L, candleTime))
        );
    }

    private void upsertMonthly(Long etfId, Map<Object, Object> data, LocalDateTime candleTime) {
        long open = parse(data.get("open")), high = parse(data.get("high")), low = parse(data.get("low"));
        long close = parse(data.get("close")), volume = parse(data.get("volume"));
        etfCandle1moRepository.findByEtfIdAndCandleTime(etfId, candleTime).ifPresentOrElse(
                existing -> existing.updateSnapshot(open, high, low, close, volume, existing.getTradeAmount()),
                () -> etfCandle1moRepository.save(EtfCandle1mo.of(etfId, open, high, low, close, volume, 0L, candleTime))
        );
    }

    @FunctionalInterface
    private interface FlushAction {
        void run(Long etfId, Map<Object, Object> data, String snapKey);
    }
}
