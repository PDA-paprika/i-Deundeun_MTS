package com.iduenduen.mtsservice.domain.order.service;

import com.iduenduen.mtsservice.common.core.CoreAccountClient;
import com.iduenduen.mtsservice.common.core.dto.CoreAccountHoldingsResponse;
import com.iduenduen.mtsservice.common.exception.GeneralException;
import com.iduenduen.mtsservice.common.status.ErrorStatus;
import com.iduenduen.mtsservice.domain.account.dto.AccountBalanceResponse;
import com.iduenduen.mtsservice.domain.ls.order.LsOrderClient;
import com.iduenduen.mtsservice.domain.ls.order.dto.LsOrderResponse;
import com.iduenduen.mtsservice.domain.order.dto.OrderRequest;
import com.iduenduen.mtsservice.domain.order.dto.OrderResponse;
import com.iduenduen.mtsservice.domain.order.entity.TradeOrder;
import com.iduenduen.mtsservice.domain.order.enums.OrderSide;
import com.iduenduen.mtsservice.domain.order.enums.OrderStatus;
import com.iduenduen.mtsservice.domain.order.enums.OrderType;
import com.iduenduen.mtsservice.domain.order.repository.TradeOrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final TradeOrderRepository tradeOrderRepository;
    private final LsOrderClient lsOrderClient;
    private final CoreAccountClient coreAccountClient;

    @Transactional
    public OrderResponse submitOrder(OrderRequest request) {

        if (request.getSide() == OrderSide.BUY) {
            // 매수: 잔고 검증
            AccountBalanceResponse balance = coreAccountClient.getBalance(request.getAccountId());
            // TODO: 시장가 검증 - Redis etf:price:{etfId} 저장 로직 추가 후 구현 필요
            long orderAmount = request.getOrderType() == OrderType.MARKET ? 0L : request.getPrice() * request.getQty();
            if (balance.getAvailableAmt() < orderAmount) {
                throw new GeneralException(ErrorStatus.INSUFFICIENT_BALANCE);
            }
        } else {
            // 매도: 보유수량 검증
            CoreAccountHoldingsResponse holdings = coreAccountClient.getHoldings(request.getAccountId());
            int heldQty = holdings.getHoldings().stream()
                    .filter(h -> h.getEtfId().equals(request
                            .getEtfId()))
                    .mapToInt(CoreAccountHoldingsResponse.HoldingDto::getQty)
                    .sum();
            if (heldQty < request.getQty()) {
                throw new GeneralException(ErrorStatus.INSUFFICIENT_HOLDING);
            }
        }

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
