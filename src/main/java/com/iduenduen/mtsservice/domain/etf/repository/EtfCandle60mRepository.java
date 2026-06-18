package com.iduenduen.mtsservice.domain.etf.repository;

import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle60m;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EtfCandle60mRepository extends JpaRepository<EtfCandle60m, Long> {

    Optional<EtfCandle60m> findByEtfIdAndCandleTime(Long etfId, LocalDateTime candleTime);

    List<EtfCandle60m> findByEtfIdAndCandleTimeBetweenOrderByCandleTimeDesc(
            Long etfId, LocalDateTime from, LocalDateTime to, Pageable pageable);
}
