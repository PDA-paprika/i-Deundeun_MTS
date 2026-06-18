package com.iduenduen.mtsservice.domain.ls.order;

import com.iduenduen.mtsservice.domain.ls.client.LsRestClient;
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
        LsOrderRequest request = LsOrderRequest.builder()
                .accNo(accountNumber)
                .IsuNo(etfCode)
                .OrdQty(qty)
                .OrdPrc(orderType == OrderType.MARKET ? 0L : price)
                .BnsTpCode(side == OrderSide.BUY ? "02" : "01")
                .OrdprcPtnCode(orderType == OrderType.MARKET ? "03" : "00")
                .MgntrnCode("000")
                .LoanDt("")
                .OrdCndiTpCode("0")
                .build();

        return lsRestClient.post(
                "/stock/order",
                "CSPAT00601",
                request,
                LsOrderResponse.class
        );
    }
}
