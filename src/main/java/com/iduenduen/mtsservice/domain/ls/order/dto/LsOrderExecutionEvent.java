package com.iduenduen.mtsservice.domain.ls.order.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LsOrderExecutionEvent {
    private String ordxctptncode;  // 주문체결유형코드 (11:체결)
    private String ordno;          // 주문번호
    private String execqty;        // 체결수량
    private String unercqty;       // 미체결수량
    private String execprc;        // 체결가격
    private String shtnIsuno;      // 단축종목번호 (ETF 코드)
    private String bnstp;          // 매매구분 (1:매도, 2:매수)
}