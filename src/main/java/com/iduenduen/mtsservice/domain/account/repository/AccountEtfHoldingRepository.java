package com.iduenduen.mtsservice.domain.account.repository;

import com.iduenduen.mtsservice.domain.account.entity.AccountEtfHolding;
import com.iduenduen.mtsservice.domain.account.entity.AccountEtfHoldingId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountEtfHoldingRepository extends JpaRepository<AccountEtfHolding, AccountEtfHoldingId> {
    List<AccountEtfHolding> findByIdAccountId(String accountId);
}
