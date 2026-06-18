package com.iduenduen.mtsservice.domain.account.repository;

import com.iduenduen.mtsservice.domain.account.entity.AccountEtfHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountEtfHistoryRepository extends JpaRepository<AccountEtfHistory, Long> {
    List<AccountEtfHistory> findByAccountIdOrderByOccurredAtDesc(Long accountId);
}
