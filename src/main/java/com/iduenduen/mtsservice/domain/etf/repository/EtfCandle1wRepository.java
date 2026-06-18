package com.iduenduen.mtsservice.domain.etf.repository;

import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1w;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EtfCandle1wRepository extends JpaRepository<EtfCandle1w, Long> {

    Optional<EtfCandle1w> findByEtfIdAndCandleTime(Long etfId, LocalDateTime candleTime);

    List<EtfCandle1w> findByEtfIdOrderByCandleTimeDesc(Long etfId);

    List<EtfCandle1w> findByEtfIdAndCandleTimeBetweenOrderByCandleTimeDesc(
            Long etfId, LocalDateTime from, LocalDateTime to, Pageable pageable);
}
