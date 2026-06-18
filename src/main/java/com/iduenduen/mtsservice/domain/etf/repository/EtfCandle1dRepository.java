package com.iduenduen.mtsservice.domain.etf.repository;

import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1d;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EtfCandle1dRepository extends JpaRepository<EtfCandle1d, UUID> {

    Optional<EtfCandle1d> findByEtfIdAndCandleTime(UUID etfId, LocalDateTime candleTime);

    List<EtfCandle1d> findTop2ByEtfIdOrderByCandleTimeDesc(UUID etfId);
}
