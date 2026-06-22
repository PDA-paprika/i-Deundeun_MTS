package com.iduenduen.mtsservice.domain.order.service;

import com.iduenduen.mtsservice.common.core.CoreAccountClient;
import com.iduenduen.mtsservice.common.core.dto.CoreAccountHoldingsResponse;
import com.iduenduen.mtsservice.common.exception.GeneralException;
import com.iduenduen.mtsservice.common.status.ErrorStatus;
import com.iduenduen.mtsservice.domain.account.dto.AccountBalanceResponse;
import com.iduenduen.mtsservice.domain.ls.order.LsOrderClient;
import com.iduenduen.mtsservice.domain.ls.order.dto.LsOrderExecutionEvent;
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
    public OrderResponse submitOrder(OrderRequest request, String authHeader) {

        if (request.getSide() == OrderSide.BUY) {
            AccountBalanceResponse balance = coreAccountClient.getBalance(request.getAccountId(), authHeader);
            long orderAmount = request.getOrderType() == OrderType.MARKET ? 0L : request.getPrice() * request.getQty();
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

        LsOrderResponse lsResponse = lsOrderClient.submitOrder(
                request.getAccountNumber(),
                request.getEtfCode(),
                request.getSide(),
                request.getOrderType(),
                request.getPrice(),
                request.getQty()
        );

        TradeOrder order = TradeOrder.builder()
                .accountId(request.getAccountId())
                .parentId(request.getParentId())
                .etfId(request.getEtfId())
                .side(request.getSide())
                .orderType(request.getOrderType())
                .price(request.getPrice())
                .qty(request.getQty())
                .remainingQty(request.getQty())
                .status(OrderStatus.PENDING)
                .lsOrdno(lsResponse.getOrdNo())
                .build();
        tradeOrderRepository.save(order);

        return OrderResponse.builder()
                .orderId(order.getId())
                .status("ACCEPTED")
                .message("주문이 접수되었습니다.")
                .build();
    }

    @Transactional
    public void processExecution(LsOrderExecutionEvent event) {
        if (!"11".equals(event.getOrdxctptncode())) return;

        String lsOrdno = event.getOrdno().trim();
        int filledQty = Integer.parseInt(event.getExecqty().trim());

        tradeOrderRepository.findByLsOrdno(lsOrdno).ifPresent(order -> {
            order.fill(filledQty);
        });
    }
}