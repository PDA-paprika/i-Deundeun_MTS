package com.iduenduen.mtsservice.domain.etf.repository;

import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1mo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EtfCandle1moRepository extends JpaRepository<EtfCandle1mo, Long> {

    Optional<EtfCandle1mo> findByEtfIdAndCandleTime(Long etfId, LocalDateTime candleTime);

    List<EtfCandle1mo> findByEtfIdOrderByCandleTimeDesc(Long etfId);

    boolean existsByEtfId(Long etfId);

    List<EtfCandle1mo> findByEtfIdAndCandleTimeBetweenOrderByCandleTimeDesc(
            Long etfId, LocalDateTime from, LocalDateTime to, Pageable pageable);
}
