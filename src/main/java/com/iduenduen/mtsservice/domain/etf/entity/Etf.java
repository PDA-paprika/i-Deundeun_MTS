package com.iduenduen.mtsservice.domain.etf.entity;

import com.iduenduen.mtsservice.common.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "etfs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Etf extends BaseEntity {

    private static final String DEFAULT_LOGO_IMG = "신한이미지";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "logo_img", nullable = false, length = 250)
    private String logoImg;

    @Column(name = "listing", nullable = false)
    private long listing;

    private Etf(String code, String name, String logoImg) {
        this.code = code;
        this.name = name;
        this.logoImg = logoImg;
    }

    public static Etf create(String code, String name) {
        return new Etf(code, name, DEFAULT_LOGO_IMG);
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateListing(long listing) {
        this.listing = listing;
    }
}
