package com.iduenduen.mtsservice.domain.etf.repository;

import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle30m;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EtfCandle30mRepository extends JpaRepository<EtfCandle30m, Long> {

    Optional<EtfCandle30m> findByEtfIdAndCandleTime(Long etfId, LocalDateTime candleTime);

    List<EtfCandle30m> findByEtfIdAndCandleTimeBetweenOrderByCandleTimeDesc(
            Long etfId, LocalDateTime from, LocalDateTime to, Pageable pageable);
}
