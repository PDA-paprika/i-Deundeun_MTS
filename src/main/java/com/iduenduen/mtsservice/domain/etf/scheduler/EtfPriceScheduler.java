package com.iduenduen.mtsservice.domain.etf.scheduler;

import com.iduenduen.mtsservice.domain.etf.entity.Etf;
import com.iduenduen.mtsservice.domain.etf.repository.EtfRepository;
import com.iduenduen.mtsservice.domain.etf.service.EtfPriceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EtfPriceScheduler {

    private static final long DELAY_BETWEEN_CALLS_MS = 1000;

    private final EtfRepository etfRepository;
    private final EtfPriceService etfPriceService;

    @Scheduled(fixedDelay = 60_000)
    public void collectCurrentPrices() throws InterruptedException {
        for (Etf etf : etfRepository.findAll()) {
            try {
                etfPriceService.fetchAndSaveCurrentPrice(etf.getCode());
            } catch (Exception e) {
          //      log.warn("[*] ETF 시세 수집 실패. code={} - {}", etf.getCode(), e.getMessage());
            }
            Thread.sleep(DELAY_BETWEEN_CALLS_MS);
        }
    }
}
