package com.iduenduen.mtsservice.domain.etf.repository;

import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1d;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface EtfCandle1dRepository extends JpaRepository<EtfCandle1d, Long> {

    Optional<EtfCandle1d> findByEtfIdAndCandleTime(Long etfId, LocalDateTime candleTime);

    List<EtfCandle1d> findTop2ByEtfIdOrderByCandleTimeDesc(Long etfId);

    List<EtfCandle1d> findByEtfIdAndCandleTimeBetweenOrderByCandleTimeDesc(
            Long etfId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    @Query("SELECT AVG(c.closePrice) FROM EtfCandle1d c WHERE c.etfId = :etfId AND c.candleTime BETWEEN :from AND :to")
    Double findAverageClosePriceByEtfIdAndPeriod(
            @Param("etfId") Long etfId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
