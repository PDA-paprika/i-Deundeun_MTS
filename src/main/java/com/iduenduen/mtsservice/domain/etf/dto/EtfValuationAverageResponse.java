package com.iduenduen.mtsservice.domain.etf.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class EtfValuationAverageResponse {
    private String etfCode;
    private LocalDate from;
    private LocalDate to;
    private Long averagePrice;
    private int dataCount;
}
