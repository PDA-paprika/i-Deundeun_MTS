package com.iduenduen.mtsservice.domain.account.repository;

import com.iduenduen.mtsservice.domain.account.entity.AccountCashHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountCashHistoryRepository extends JpaRepository<AccountCashHistory, Long> {
    List<AccountCashHistory> findByAccountIdOrderByOccurredAtDesc(Long accountId);
}
