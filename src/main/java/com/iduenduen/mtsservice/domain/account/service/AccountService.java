package com.iduenduen.mtsservice.domain.account.service;

import com.iduenduen.mtsservice.common.core.CoreAccountClient;
import com.iduenduen.mtsservice.domain.account.dto.AccountBalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final CoreAccountClient coreAccountClient;

    public AccountBalanceResponse getBalance(Long accountId) {
        return coreAccountClient.getBalance(accountId);
    }
}