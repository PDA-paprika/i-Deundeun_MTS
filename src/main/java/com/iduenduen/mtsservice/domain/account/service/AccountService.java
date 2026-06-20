package com.iduenduen.mtsservice.domain.account.service;

import com.iduenduen.mtsservice.common.core.CoreAccountClient;
import com.iduenduen.mtsservice.common.core.dto.CoreAccountHoldingsResponse;
import com.iduenduen.mtsservice.domain.account.dto.AccountBalanceResponse;
import com.iduenduen.mtsservice.domain.account.dto.AccountSummaryResponse;
import com.iduenduen.mtsservice.domain.etf.entity.Etf;
import com.iduenduen.mtsservice.domain.etf.repository.EtfRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final CoreAccountClient coreAccountClient;
    private final EtfRepository etfRepository;

    public AccountBalanceResponse getBalance(Long accountId) {
        return coreAccountClient.getBalance(accountId);
    }

    public AccountSummaryResponse getSummary(Long accountId) {
        CoreAccountHoldingsResponse coreResponse = coreAccountClient.getHoldings(accountId);

        List<AccountSummaryResponse.HoldingDto> holdingDtos = coreResponse.getHoldings().stream()
                .map(h -> {
                    Etf etf = etfRepository.findById(h.getEtfId())
                            .orElseThrow(() -> new RuntimeException("ETF not found: " + h.getEtfId()));
                    return AccountSummaryResponse.HoldingDto.builder()
                            .etfId(h.getEtfId())
                            .etfCode(etf.getCode())
                            .etfName(etf.getName())
                            .logoImg(etf.getLogoImg())
                            .qty(h.getQty())
                            .avgBuyPrice(h.getAvgBuyPrice())
                            .build();
                })
                .toList();

        long totalPurchaseAmount = holdingDtos.stream()
                .mapToLong(h -> h.getAvgBuyPrice() * h.getQty())
                .sum();

        return AccountSummaryResponse.builder()
                .availableAmt(coreResponse.getAvailableAmt())
                .totalPurchaseAmount(totalPurchaseAmount)
                .holdings(holdingDtos)
                .build();
    }
}