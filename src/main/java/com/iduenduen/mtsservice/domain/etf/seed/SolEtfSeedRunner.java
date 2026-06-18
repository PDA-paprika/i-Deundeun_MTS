package com.iduenduen.mtsservice.domain.etf.seed;

import com.iduenduen.mtsservice.domain.etf.repository.EtfRepository;
import com.iduenduen.mtsservice.domain.etf.service.EtfPriceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class SolEtfSeedRunner implements ApplicationRunner {

    private static final long DELAY_BETWEEN_CALLS_MS = 1000;

    private final EtfRepository etfRepository;
    private final EtfPriceService etfPriceService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (etfRepository.count() >= SolEtfCodes.CODES.size()) {
            log.info("[*] SOL ETF 시딩 스킵 (이미 전종목 시딩 완료).");
            return;
        }

        Set<String> existingCodes = etfRepository.findAll().stream()
                .map(etf -> etf.getCode())
                .collect(java.util.stream.Collectors.toSet());

        for (String code : SolEtfCodes.CODES) {
            if (existingCodes.contains(code)) {
                continue;
            }
            try {
                etfPriceService.fetchAndSaveCurrentPrice(code);
                log.info("[*] SOL ETF 시딩 완료. code={}", code);
            } catch (Exception e) {
            //    log.warn("[*] SOL ETF 시딩 실패. code={} - {}", code, e.getMessage());
            }
            Thread.sleep(DELAY_BETWEEN_CALLS_MS);
        }
    }
}
