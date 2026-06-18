package com.iduenduen.mtsservice.domain.etf.repository;

import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1d;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;


public interface EtfCandle1dRepository extends JpaRepository<EtfCandle1d, Long> {

    Optional<EtfCandle1d> findByEtfIdAndCandleTime(Long etfId, LocalDateTime candleTime);
}
