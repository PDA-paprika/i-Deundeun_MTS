package com.iduenduen.mtsservice.domain.etf.service;

import com.iduenduen.mtsservice.common.exception.GeneralException;
import com.iduenduen.mtsservice.common.status.ErrorStatus;
import com.iduenduen.mtsservice.domain.etf.entity.Etf;
import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1d;
import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1m;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1dRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1mRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfRepository;
import com.iduenduen.mtsservice.domain.etf.seed.SolEtfCodes;
import com.iduenduen.mtsservice.common.ls.stock.service.LsStockPriceService;
import com.iduenduen.mtsservice.common.ls.stock.dto.LsStockPriceResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EtfPriceService {

    private static final long MILLION = 1_000_000L;
    private static final long THOUSAND = 1_000L;

    private final LsStockPriceService lsStockPriceService;
    private final EtfRepository etfRepository;
    private final EtfCandle1mRepository etfCandle1mRepository;
    private final EtfCandle1dRepository etfCandle1dRepository;

    @Transactional
    public EtfCandle1m fetchAndSaveCurrentPrice(String code) {
        if (!SolEtfCodes.CODES.contains(code)) {
            throw new GeneralException(ErrorStatus.ETF_NOT_FOUND);
        }

        LsStockPriceResponse.T1901OutBlock outBlock;
        try {
            outBlock = lsStockPriceService.getCurrentPrice(code);
        } catch (RestClientException e) {
            throw new GeneralException(ErrorStatus.LS_API_ERROR);
        }

        Etf etf = etfRepository.findByCode(code)
                .orElseGet(() -> etfRepository.save(Etf.create(code, outBlock.getHname())));
        etf.updateListing(Long.parseLong(outBlock.getListing()) * THOUSAND);

        long openPrice = Long.parseLong(outBlock.getOpen());
        long highPrice = Long.parseLong(outBlock.getHigh());
        long lowPrice = Long.parseLong(outBlock.getLow());
        long closePrice = Long.parseLong(outBlock.getPrice());
        long volume = Long.parseLong(outBlock.getVolume());
        long tradeAmount = Long.parseLong(outBlock.getValue()) * MILLION;

        EtfCandle1m candle = EtfCandle1m.of(
                etf.getId(), openPrice, highPrice, lowPrice, closePrice, volume, tradeAmount,
                LocalDateTime.now().withSecond(0).withNano(0)
        );
        etfCandle1mRepository.save(candle);

        upsertTodayCandle(etf.getId(), openPrice, highPrice, lowPrice, closePrice, volume, tradeAmount);

        return candle;
    }

    private void upsertTodayCandle(UUID etfId, long openPrice, long highPrice, long lowPrice,
                                    long closePrice, long volume, long tradeAmount) {
        LocalDateTime today = LocalDate.now().atStartOfDay();

        etfCandle1dRepository.findByEtfIdAndCandleTime(etfId, today)
                .ifPresentOrElse(
                        existing -> existing.updateSnapshot(openPrice, highPrice, lowPrice, closePrice, volume, tradeAmount),
                        () -> etfCandle1dRepository.save(
                                EtfCandle1d.of(etfId, openPrice, highPrice, lowPrice, closePrice, volume, tradeAmount, today)
                        )
                );
    }
}
