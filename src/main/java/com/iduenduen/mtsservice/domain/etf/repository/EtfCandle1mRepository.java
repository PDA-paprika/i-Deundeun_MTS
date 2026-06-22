package com.iduenduen.mtsservice.domain.etf.repository;

import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1m;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EtfCandle1mRepository extends JpaRepository<EtfCandle1m, Long> {

    Optional<EtfCandle1m> findByEtfIdAndCandleTime(Long etfId, LocalDateTime candleTime);

    List<EtfCandle1m> findByEtfIdAndCandleTimeBetweenOrderByCandleTimeDesc(
            Long etfId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    boolean existsByEtfId(Long etfId);
}
