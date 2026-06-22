package com.iduenduen.mtsservice.domain.etf.service;

import com.iduenduen.mtsservice.common.exception.GeneralException;
import com.iduenduen.mtsservice.common.ls.stock.dto.LsOrderBookResponse;
import com.iduenduen.mtsservice.common.ls.stock.service.LsOrderBookService;
import com.iduenduen.mtsservice.common.status.ErrorStatus;
import com.iduenduen.mtsservice.domain.etf.dto.EtfOrderBookResponse;
import com.iduenduen.mtsservice.domain.etf.dto.OrderBookLevel;
import com.iduenduen.mtsservice.domain.etf.entity.Etf;
import com.iduenduen.mtsservice.domain.etf.entity.OrderBookSnapshot;
import com.iduenduen.mtsservice.domain.etf.repository.EtfRepository;
import com.iduenduen.mtsservice.domain.etf.repository.OrderBookSnapshotRepository;
import com.iduenduen.mtsservice.domain.etf.seed.SolEtfCodes;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EtfOrderBookService {

    private static final int LEVELS = 5;

    private final LsOrderBookService lsOrderBookService;
    private final EtfRepository etfRepository;
    private final OrderBookSnapshotRepository orderBookSnapshotRepository;
    private final EtfRealtimeCacheService etfRealtimeCacheService;

    @Transactional
    public void fetchAndSaveSnapshot(String code) {
        if (!SolEtfCodes.CODES.contains(code)) {
            throw new GeneralException(ErrorStatus.ETF_NOT_FOUND);
        }

        Etf etf = etfRepository.findByCode(code)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ETF_NOT_FOUND));

        LsOrderBookResponse.T1101OutBlock outBlock;
        try {
            outBlock = lsOrderBookService.getOrderBook(code);
        } catch (RestClientException e) {
            throw new GeneralException(ErrorStatus.LS_API_ERROR);
        }

        persistSnapshot(
                etf.getId(),
                new long[]{parse(outBlock.getOfferho1()), parse(outBlock.getOfferho2()), parse(outBlock.getOfferho3()), parse(outBlock.getOfferho4()), parse(outBlock.getOfferho5())},
                new long[]{parse(outBlock.getOfferrem1()), parse(outBlock.getOfferrem2()), parse(outBlock.getOfferrem3()), parse(outBlock.getOfferrem4()), parse(outBlock.getOfferrem5())},
                new long[]{parse(outBlock.getBidho1()), parse(outBlock.getBidho2()), parse(outBlock.getBidho3()), parse(outBlock.getBidho4()), parse(outBlock.getBidho5())},
                new long[]{parse(outBlock.getBidrem1()), parse(outBlock.getBidrem2()), parse(outBlock.getBidrem3()), parse(outBlock.getBidrem4()), parse(outBlock.getBidrem5())}
        );
    }

    @Transactional
    public void applyRealtimeOrderBook(String code, long[] askPrices, long[] askQtys, long[] bidPrices, long[] bidQtys) {
        Etf etf = etfRepository.findByCode(code)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ETF_NOT_FOUND));

        persistSnapshot(etf.getId(), askPrices, askQtys, bidPrices, bidQtys);
    }

    private void persistSnapshot(Long etfId, long[] askPrices, long[] askQtys, long[] bidPrices, long[] bidQtys) {
        LocalDateTime snapshotAt = LocalDateTime.now().withNano(0);
        List<OrderBookSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < LEVELS; i++) {
            snapshots.add(OrderBookSnapshot.of(etfId, OrderBookSnapshot.SIDE_ASK, i + 1, askPrices[i], askQtys[i], snapshotAt));
            snapshots.add(OrderBookSnapshot.of(etfId, OrderBookSnapshot.SIDE_BID, i + 1, bidPrices[i], bidQtys[i], snapshotAt));
        }
        orderBookSnapshotRepository.saveAll(snapshots);
    }

    private long parse(String value) {
        return Long.parseLong(value);
    }

    @Transactional(readOnly = true)
    public EtfOrderBookResponse getOrderBook(String code) {
        Optional<EtfRealtimeCacheService.OrderBookSnapshotCache> cached = etfRealtimeCacheService.getCachedOrderBook(code);
        if (cached.isPresent()) {
            return fromCache(code, cached.get());
        }

        Etf etf = etfRepository.findByCode(code)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ETF_NOT_FOUND));

        LocalDateTime snapshotAt = orderBookSnapshotRepository.findLatestSnapshotAt(etf.getId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.ETF_NOT_FOUND));

        List<OrderBookSnapshot> rows = orderBookSnapshotRepository.findByEtfIdAndSnapshotAt(etf.getId(), snapshotAt);

        List<OrderBookLevel> asks = rows.stream()
                .filter(row -> OrderBookSnapshot.SIDE_ASK.equals(row.getSide()))
                .sorted(Comparator.comparingInt(OrderBookSnapshot::getStep))
                .map(row -> OrderBookLevel.of(row.getStep(), row.getPrice(), row.getQty()))
                .toList();

        List<OrderBookLevel> bids = rows.stream()
                .filter(row -> OrderBookSnapshot.SIDE_BID.equals(row.getSide()))
                .sorted(Comparator.comparingInt(OrderBookSnapshot::getStep))
                .map(row -> OrderBookLevel.of(row.getStep(), row.getPrice(), row.getQty()))
                .toList();

        return EtfOrderBookResponse.of(code, snapshotAt, asks, bids);
    }

    private EtfOrderBookResponse fromCache(String code, EtfRealtimeCacheService.OrderBookSnapshotCache cache) {
        List<OrderBookLevel> asks = new ArrayList<>();
        List<OrderBookLevel> bids = new ArrayList<>();
        for (int i = 0; i < LEVELS; i++) {
            int step = i + 1;
            asks.add(OrderBookLevel.of(step, cache.askPrices()[i], cache.askQtys()[i]));
            bids.add(OrderBookLevel.of(step, cache.bidPrices()[i], cache.bidQtys()[i]));
        }
        return EtfOrderBookResponse.of(code, cache.updatedAt(), asks, bids);
    }
}
