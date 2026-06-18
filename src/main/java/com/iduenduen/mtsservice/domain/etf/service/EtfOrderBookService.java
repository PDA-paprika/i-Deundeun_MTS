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
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EtfOrderBookService {

    private static final int LEVELS = 5;

    private final LsOrderBookService lsOrderBookService;
    private final EtfRepository etfRepository;
    private final OrderBookSnapshotRepository orderBookSnapshotRepository;

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

        LocalDateTime snapshotAt = LocalDateTime.now().withNano(0);
        List<OrderBookSnapshot> snapshots = List.of(
                snapshot(etf.getId(), OrderBookSnapshot.SIDE_ASK, 1, outBlock.getOfferho1(), outBlock.getOfferrem1(), snapshotAt),
                snapshot(etf.getId(), OrderBookSnapshot.SIDE_ASK, 2, outBlock.getOfferho2(), outBlock.getOfferrem2(), snapshotAt),
                snapshot(etf.getId(), OrderBookSnapshot.SIDE_ASK, 3, outBlock.getOfferho3(), outBlock.getOfferrem3(), snapshotAt),
                snapshot(etf.getId(), OrderBookSnapshot.SIDE_ASK, 4, outBlock.getOfferho4(), outBlock.getOfferrem4(), snapshotAt),
                snapshot(etf.getId(), OrderBookSnapshot.SIDE_ASK, 5, outBlock.getOfferho5(), outBlock.getOfferrem5(), snapshotAt),
                snapshot(etf.getId(), OrderBookSnapshot.SIDE_BID, 1, outBlock.getBidho1(), outBlock.getBidrem1(), snapshotAt),
                snapshot(etf.getId(), OrderBookSnapshot.SIDE_BID, 2, outBlock.getBidho2(), outBlock.getBidrem2(), snapshotAt),
                snapshot(etf.getId(), OrderBookSnapshot.SIDE_BID, 3, outBlock.getBidho3(), outBlock.getBidrem3(), snapshotAt),
                snapshot(etf.getId(), OrderBookSnapshot.SIDE_BID, 4, outBlock.getBidho4(), outBlock.getBidrem4(), snapshotAt),
                snapshot(etf.getId(), OrderBookSnapshot.SIDE_BID, 5, outBlock.getBidho5(), outBlock.getBidrem5(), snapshotAt)
        );

        orderBookSnapshotRepository.saveAll(snapshots);
    }

    @Transactional(readOnly = true)
    public EtfOrderBookResponse getOrderBook(String code) {
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

    private OrderBookSnapshot snapshot(Long etfId, String side, int step, String price, String qty, LocalDateTime snapshotAt) {
        return OrderBookSnapshot.of(etfId, side, step, Long.parseLong(price), Long.parseLong(qty), snapshotAt);
    }
}
