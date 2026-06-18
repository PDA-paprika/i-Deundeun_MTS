package com.iduenduen.mtsservice.domain.etf.repository;

import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle10m;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EtfCandle10mRepository extends JpaRepository<EtfCandle10m, Long> {

    Optional<EtfCandle10m> findByEtfIdAndCandleTime(Long etfId, LocalDateTime candleTime);

    List<EtfCandle10m> findByEtfIdAndCandleTimeBetweenOrderByCandleTimeDesc(
            Long etfId, LocalDateTime from, LocalDateTime to, Pageable pageable);
}
