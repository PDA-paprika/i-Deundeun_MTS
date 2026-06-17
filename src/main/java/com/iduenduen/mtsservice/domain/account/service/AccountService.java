package com.iduenduen.mtsservice.domain.account.service;

import com.iduenduen.mtsservice.common.exception.GeneralException;
import com.iduenduen.mtsservice.common.status.ErrorStatus;
import com.iduenduen.mtsservice.domain.account.dto.AccountBalanceResponse;
import com.iduenduen.mtsservice.domain.account.entity.Account;
import com.iduenduen.mtsservice.domain.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountBalanceResponse getBalance(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ACCOUNT_NOT_FOUND));

        return AccountBalanceResponse.builder()
                .availableCash(account.getAvailableAmt())
                .d0Balance(account.getAvailableAmt())
                .d1Balance(account.getAvailableAmt())
                .d2Balance(account.getAvailableAmt())
                .build();
    }
}
