package com.iduenduen.mtsservice.domain.order.service;

import com.iduenduen.mtsservice.common.core.CoreAccountClient;
import com.iduenduen.mtsservice.common.core.dto.CoreAccountHoldingsResponse;
import com.iduenduen.mtsservice.common.core.dto.CoreTradeRequest;
import com.iduenduen.mtsservice.common.exception.GeneralException;
import com.iduenduen.mtsservice.common.status.ErrorStatus;
import com.iduenduen.mtsservice.domain.account.dto.AccountBalanceResponse;
import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1d;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1dRepository;
import com.iduenduen.mtsservice.domain.etf.service.EtfRealtimeCacheService;
import com.iduenduen.mtsservice.domain.order.dto.OrderRequest;
import com.iduenduen.mtsservice.domain.order.dto.OrderResponse;
import com.iduenduen.mtsservice.domain.order.entity.TradeOrder;
import com.iduenduen.mtsservice.domain.order.enums.OrderSide;
import com.iduenduen.mtsservice.domain.order.enums.OrderStatus;
import com.iduenduen.mtsservice.domain.order.enums.OrderType;
import com.iduenduen.mtsservice.domain.order.realtime.OrderExecutionWebSocketHandler;
import com.iduenduen.mtsservice.domain.order.repository.TradeOrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final TradeOrderRepository tradeOrderRepository;
    private final CoreAccountClient coreAccountClient;
    private final EtfRealtimeCacheService etfRealtimeCacheService;
    private final OrderExecutionWebSocketHandler orderExecutionWebSocketHandler;
    private final EtfCandle1dRepository etfCandle1dRepository;

    @Transactional
    public OrderResponse submitOrder(OrderRequest request, String authHeader) {

        LocalTime now = LocalTime.now(ZoneId.of("Asia/Seoul"));
        if (now.isBefore(LocalTime.of(9, 0)) || now.isAfter(LocalTime.of(15, 30))) {
            throw new GeneralException(ErrorStatus.MARKET_CLOSED);
        }

        if (request.getSide() == OrderSide.BUY) {
            AccountBalanceResponse balance = coreAccountClient.getBalance(request.getAccountId(), authHeader);
            long orderAmount = etfRealtimeCacheService.getCachedPrice(request.getEtfCode())
                    .map(p -> p.price() * request.getQty())
                    .orElse(0L);
            if (balance.getAvailableAmt() < orderAmount) {
                throw new GeneralException(ErrorStatus.INSUFFICIENT_BALANCE);
            }
        } else {
            CoreAccountHoldingsResponse holdings = coreAccountClient.getHoldings(request.getAccountId(), authHeader);
            int heldQty = holdings.getHoldings().stream()
                    .filter(h -> h.getEtfId().equals(request.getEtfId()))
                    .mapToInt(CoreAccountHoldingsResponse.HoldingDto::getQty)
                    .sum();
            if (heldQty < request.getQty()) {
                throw new GeneralException(ErrorStatus.INSUFFICIENT_HOLDING);
            }
        }

        long execPrice = resolveExecPrice(request);

        TradeOrder order = TradeOrder.builder()
                .accountId(request.getAccountId())
                .parentId(request.getParentId())
                .etfId(request.getEtfId())
                .side(request.getSide())
                .orderType(request.getOrderType())
                .price(execPrice)
                .qty(request.getQty())
                .remainingQty(request.getQty())
                .status(OrderStatus.PENDING)
                .childId(request.getChildId())
                .goalId(request.getGoalId())
                .build();
        tradeOrderRepository.save(order);

        execute(order, request.getEtfCode(), execPrice, request.getEtfName(), request.getLinkId());

        return OrderResponse.builder()
                .orderId(order.getId())
                .status("EXECUTED")
                .message("주문이 체결되었습니다.")
                .build();
    }

    private long resolveExecPrice(OrderRequest request) {
        if (request.getOrderType() == OrderType.MARKET) {
            return etfRealtimeCacheService.getCachedPrice(request.getEtfCode())
                    .map(EtfRealtimeCacheService.PriceSnapshot::price)
                    .orElseGet(() -> {
                        List<EtfCandle1d> candles = etfCandle1dRepository
                                .findTop2ByEtfIdOrderByCandleTimeDesc(request.getEtfId());
                        if (!candles.isEmpty()) {
                            return candles.get(0).getClosePrice();
                        }
                        throw new GeneralException(ErrorStatus.PRICE_NOT_AVAILABLE);
                    });
        }
        return request.getPrice() != null ? request.getPrice() : 0L;
    }

    private void execute(TradeOrder order, String etfCode, long execPrice, String etfName, Long linkId) {
        int qty = order.getQty();
        order.fill(qty);

        CoreTradeRequest coreReq = CoreTradeRequest.builder()
                .accountId(order.getAccountId())
                .parentId(order.getParentId())
                .etfId(order.getEtfId())
                .etfName(etfName)
                .eventType(order.getSide().name())
                .qty(qty)
                .price(execPrice)
                .referenceId(order.getId().toString())
                .referenceType("EXECUTION")
                .occurredAt(LocalDateTime.now())
                .childId(order.getChildId())
                .goalId(order.getGoalId())
                .linkId(linkId)
                .build();

        try {
            coreAccountClient.postTrade(coreReq);
        } catch (Exception e) {
            log.warn("Core postTrade failed. orderId={}", order.getId(), e);
        }

        orderExecutionWebSocketHandler.pushExecution(
                order.getAccountId(),
                order.getId(),
                etfCode,
                order.getSide().name(),
                qty,
                execPrice
        );
    }
}