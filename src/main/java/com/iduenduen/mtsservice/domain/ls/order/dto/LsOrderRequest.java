package com.iduenduen.mtsservice.domain.ls.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LsOrderRequest {

    @JsonProperty("CSPAT00601InBlock1")
    private Block block;

    @Getter
    @Builder
    public static class Block {
        @JsonProperty("AccNo")
        private String accNo;
        @JsonProperty("IsuNo")
        private String isuNo;
        @JsonProperty("OrdQty")
        private int ordQty;
        @JsonProperty("OrdPrc")
        private long ordPrc;
        @JsonProperty("BnsTpCode")
        private String bnsTpCode;
        @JsonProperty("OrdprcPtnCode")
        private String ordprcPtnCode;
        @JsonProperty("MgntrnCode")
        private String mgntrnCode;
        @JsonProperty("LoanDt")
        private String loanDt;
        @JsonProperty("OrdCndiTpCode")
        private String ordCndiTpCode;
    }
}