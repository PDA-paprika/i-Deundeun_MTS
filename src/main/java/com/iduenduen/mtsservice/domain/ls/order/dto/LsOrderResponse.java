package com.iduenduen.mtsservice.domain.ls.order.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LsOrderResponse {
    private String OrdNo;    // 주문번호
    private String PrntOrdNo; // 모주문번호
}
