package com.iduenduen.mtsservice.domain.etf.repository;

import com.iduenduen.mtsservice.domain.etf.entity.Etf;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EtfRepository extends JpaRepository<Etf, UUID> {

    Optional<Etf> findByCode(String code);

    List<Etf> findByNameContainingIgnoreCase(String name);
}
