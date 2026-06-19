package com.iduenduen.mtsservice.domain.order.service;

import com.iduenduen.mtsservice.domain.ls.order.LsOrderClient;
import com.iduenduen.mtsservice.domain.ls.order.dto.LsOrderResponse;
import com.iduenduen.mtsservice.domain.order.dto.OrderRequest;
import com.iduenduen.mtsservice.domain.order.dto.OrderResponse;
import com.iduenduen.mtsservice.domain.order.entity.TradeOrder;
import com.iduenduen.mtsservice.domain.order.enums.OrderStatus;
import com.iduenduen.mtsservice.domain.order.repository.TradeOrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final TradeOrderRepository tradeOrderRepository;
    private final LsOrderClient lsOrderClient;

    @Transactional
    public OrderResponse submitOrder(OrderRequest request) {
        // TODO: Core 서버 API 호출로 잔고/보유수량 검증 필요

        // LS API 주문 제출
        LsOrderResponse lsResponse = lsOrderClient.submitOrder(
                request.getAccountNumber(),
                request.getEtfCode(),
                request.getSide(),
                request.getOrderType(),
                request.getPrice(),
                request.getQty()
        );

        // 주문 저장
        TradeOrder order = TradeOrder.builder()
                .accountId(request.getAccountId())
                .parentId(request.getParentId())
                .etfId(0L) // TODO: ETF 조회 후 수정
                .side(request.getSide())
                .orderType(request.getOrderType())
                .price(request.getPrice())
                .qty(request.getQty())
                .remainingQty(request.getQty())
                .status(OrderStatus.PENDING)
                .build();
        tradeOrderRepository.save(order);

        return OrderResponse.builder()
                .orderId(order.getId())
                .status("ACCEPTED")
                .message("주문이 접수되었습니다.")
                .build();
    }
}
