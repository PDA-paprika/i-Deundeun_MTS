package com.iduenduen.mtsservice.domain.etf.service;

import com.iduenduen.mtsservice.domain.etf.entity.Etf;
import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1m;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1mRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfRepository;
import com.iduenduen.mtsservice.domain.ls.stock.LsStockPriceService;
import com.iduenduen.mtsservice.domain.ls.stock.dto.LsStockPriceResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EtfPriceService {

    private final LsStockPriceService lsStockPriceService;
    private final EtfRepository etfRepository;
    private final EtfCandle1mRepository etfCandle1mRepository;

    @Transactional
    public EtfCandle1m fetchAndSaveCurrentPrice(String code) {
        LsStockPriceResponse.T1901OutBlock outBlock = lsStockPriceService.getCurrentPrice(code);

        Etf etf = etfRepository.findByCode(code)
                .orElseGet(() -> etfRepository.save(Etf.create(code, outBlock.getHname())));

        EtfCandle1m candle = EtfCandle1m.of(
                etf.getId(),
                Long.parseLong(outBlock.getOpen()),
                Long.parseLong(outBlock.getHigh()),
                Long.parseLong(outBlock.getLow()),
                Long.parseLong(outBlock.getPrice()),
                Long.parseLong(outBlock.getVolume()),
                LocalDateTime.now().withSecond(0).withNano(0)
        );

        return etfCandle1mRepository.save(candle);
    }
}
