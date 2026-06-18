package com.iduenduen.mtsservice.domain.order.repository;

import com.iduenduen.mtsservice.domain.order.entity.ExecutionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ExecutionHistoryRepository extends JpaRepository<ExecutionHistory, Long> {
    List<ExecutionHistory> findByAccountIdOrderByExecutedAtDesc(Long accountId);
    List<ExecutionHistory> findByAccountIdAndExecutedAtBetweenOrderByExecutedAtDesc(
            Long accountId, LocalDateTime from, LocalDateTime to);
}
