package com.iduenduen.mtsservice.domain.etf.repository;

import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1m;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EtfCandle1mRepository extends JpaRepository<EtfCandle1m, UUID> {
}
