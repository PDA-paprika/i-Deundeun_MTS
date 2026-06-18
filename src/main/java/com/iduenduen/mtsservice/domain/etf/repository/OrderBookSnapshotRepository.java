package com.iduenduen.mtsservice.domain.etf.repository;

import com.iduenduen.mtsservice.domain.etf.entity.OrderBookSnapshot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderBookSnapshotRepository extends JpaRepository<OrderBookSnapshot, Long> {

    @Query("SELECT MAX(o.snapshotAt) FROM OrderBookSnapshot o WHERE o.etfId = :etfId")
    Optional<LocalDateTime> findLatestSnapshotAt(@Param("etfId") Long etfId);

    List<OrderBookSnapshot> findByEtfIdAndSnapshotAt(Long etfId, LocalDateTime snapshotAt);
}
