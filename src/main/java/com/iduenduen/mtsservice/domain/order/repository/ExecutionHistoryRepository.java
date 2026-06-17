package com.iduenduen.mtsservice.domain.order.repository;

import com.iduenduen.mtsservice.domain.order.entity.ExecutionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ExecutionHistoryRepository extends JpaRepository<ExecutionHistory, String> {
    List<ExecutionHistory> findByAccountIdOrderByExecutedAtDesc(String accountId);
    List<ExecutionHistory> findByAccountIdAndExecutedAtBetweenOrderByExecutedAtDesc(
            String accountId, LocalDateTime from, LocalDateTime to);
}
