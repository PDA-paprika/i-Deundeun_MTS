package com.iduenduen.mtsservice.domain.order.dto;

import com.iduenduen.mtsservice.domain.order.enums.OrderSide;
import com.iduenduen.mtsservice.domain.order.enums.OrderType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OrderRequest {
    private Long accountId;
    private Long parentId;
    private String accountNumber;  // Core 서버 연동 전 임시로 요청에서 받음
    private String etfCode;
    private OrderSide side;
    private OrderType orderType;
    private Long price;
    private int qty;
    private Long etfId;
    private Long childId;
    private Long goalId;
}
