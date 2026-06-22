package com.iduenduen.mtsservice.domain.ls.order;

import com.iduenduen.mtsservice.common.ls.client.LsRestClient;
import com.iduenduen.mtsservice.domain.ls.order.dto.LsOrderRequest;
import com.iduenduen.mtsservice.domain.ls.order.dto.LsOrderResponse;
import com.iduenduen.mtsservice.domain.order.enums.OrderSide;
import com.iduenduen.mtsservice.domain.order.enums.OrderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LsOrderClient {

    private final LsRestClient lsRestClient;

    public LsOrderResponse submitOrder(String accountNumber, String etfCode,
                                       OrderSide side, OrderType orderType, Long price, int qty) {
        LsOrderRequest.Block block = LsOrderRequest.Block.builder()
                .accNo(accountNumber)
                .isuNo(etfCode)
                .ordQty(qty)
                .ordPrc(orderType == OrderType.MARKET ? 0L : price)
                .bnsTpCode(side == OrderSide.BUY ? "2" : "1")
                .ordprcPtnCode(orderType == OrderType.MARKET ? "03" : "00")
                .mgntrnCode("000")
                .loanDt("")
                .ordCndiTpCode("0")
                .build();

        LsOrderRequest request = LsOrderRequest.builder()
                .block(block)
                .build();

        return lsRestClient.post(
                "/stock/order",
                "CSPAT00601",
                request,
                LsOrderResponse.class
        );
    }
}
