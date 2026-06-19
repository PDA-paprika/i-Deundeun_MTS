package com.iduenduen.mtsservice.domain.ls.order.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LsOrderRequest {
    private String accNo;      // 계좌번호
    private String IsuNo;      // 종목코드
    private int OrdQty;        // 주문수량
    private long OrdPrc;       // 주문가격
    private String BnsTpCode;  // 매매구분 (02: 매수, 01: 매도)
    private String OrdprcPtnCode; // 호가유형 (00: 지정가, 03: 시장가)
    private String MgntrnCode; // 신용거래코드 (000: 보통)
    private String LoanDt;     // 대출일 (신용아니면 "")
    private String OrdCndiTpCode; // 주문조건 (0: 없음)
}