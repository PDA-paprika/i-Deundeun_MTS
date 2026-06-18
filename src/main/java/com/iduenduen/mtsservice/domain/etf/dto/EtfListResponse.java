package com.iduenduen.mtsservice.domain.etf.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class EtfListResponse {

    private final long totalCount;
    private final List<EtfListItem> etfs;

    private EtfListResponse(long totalCount, List<EtfListItem> etfs) {
        this.totalCount = totalCount;
        this.etfs = etfs;
    }

    public static EtfListResponse of(long totalCount, List<EtfListItem> etfs) {
        return new EtfListResponse(totalCount, etfs);
    }
}
