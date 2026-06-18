package com.iduenduen.mtsservice.domain.order.service;

import com.iduenduen.mtsservice.common.exception.GeneralException;
import com.iduenduen.mtsservice.common.status.ErrorStatus;
import com.iduenduen.mtsservice.domain.account.entity.Account;
import com.iduenduen.mtsservice.domain.account.entity.AccountEtfHolding;
import com.iduenduen.mtsservice.domain.account.entity.AccountEtfHoldingId;
import com.iduenduen.mtsservice.domain.account.repository.AccountEtfHoldingRepository;
import com.iduenduen.mtsservice.domain.account.repository.AccountRepository;
import com.iduenduen.mtsservice.domain.ls.order.LsOrderClient;
import com.iduenduen.mtsservice.domain.ls.order.dto.LsOrderResponse;
import com.iduenduen.mtsservice.domain.order.dto.OrderRequest;
import com.iduenduen.mtsservice.domain.order.dto.OrderResponse;
import com.iduenduen.mtsservice.domain.order.dto.PendingOrderResponse;
import com.iduenduen.mtsservice.domain.order.entity.TradeOrder;
import com.iduenduen.mtsservice.domain.order.enums.OrderSide;
import com.iduenduen.mtsservice.domain.order.enums.OrderStatus;
import com.iduenduen.mtsservice.domain.order.enums.OrderType;
import com.iduenduen.mtsservice.domain.order.repository.TradeOrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final TradeOrderRepository tradeOrderRepository;
    private final AccountRepository accountRepository;
    private final AccountEtfHoldingRepository accountEtfHoldingRepository;
    private final LsOrderClient lsOrderClient;

    @Transactional
    public OrderResponse submitOrder(OrderRequest request) {
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.ACCOUNT_NOT_FOUND));

        // 매수 시 잔고 검증
        if (request.getSide() == OrderSide.BUY) {
            long required = request.getOrderType() == OrderType.MARKET
                    ? 0L : request.getPrice() * request.getQty();
            if (account.getAvailableAmt() < required) {
                throw new GeneralException(ErrorStatus.INSUFFICIENT_BALANCE);
            }
        }

        // 매도 시 보유 수량 검증
        if (request.getSide() == OrderSide.SELL) {
            AccountEtfHoldingId holdingId = new AccountEtfHoldingId(request.getEtfCode(), request.getAccountId());
            AccountEtfHolding holding = accountEtfHoldingRepository.findById(holdingId)
                    .orElseThrow(() -> new GeneralException(ErrorStatus.INSUFFICIENT_HOLDING));
            if (holding.getQty() < request.getQty()) {
                throw new GeneralException(ErrorStatus.INSUFFICIENT_HOLDING);
            }
        }

        // LS API 주문 제출
        LsOrderResponse lsResponse = lsOrderClient.submitOrder(
                account.getAccountNumber(),
                request.getEtfCode(),
                request.getSide(),
                request.getOrderType(),
                request.getPrice(),
                request.getQty()
        );

        // 주문 저장
        TradeOrder order = TradeOrder.builder()
                .accountId(request.getAccountId())
                .parentId(account.getParentId())
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

    public List<PendingOrderResponse> getPendingOrders(String accountId, String filter) {
        List<OrderStatus> statuses = List.of(OrderStatus.PENDING, OrderStatus.PARTIALLY_EXECUTED);

        List<TradeOrder> orders;
        if ("BUY".equals(filter)) {
            orders = tradeOrderRepository.findByAccountIdAndSideAndStatusIn(accountId, OrderSide.BUY, statuses);
        } else if ("SELL".equals(filter)) {
            orders = tradeOrderRepository.findByAccountIdAndSideAndStatusIn(accountId, OrderSide.SELL, statuses);
        } else {
            orders = tradeOrderRepository.findByAccountIdAndStatusIn(accountId, statuses);
        }

        return orders.stream()
                .map(o -> PendingOrderResponse.builder()
                        .orderId(o.getId())
                        .side(o.getSide())
                        .orderType(o.getOrderType())
                        .price(o.getPrice())
                        .qty(o.getQty())
                        .remainingQty(o.getRemainingQty())
                        .status(o.getStatus())
                        .orderedAt(o.getCreatedAt())
                        .build())
                .toList();
    }
}
