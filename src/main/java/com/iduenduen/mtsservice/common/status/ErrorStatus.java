package com.iduenduen.mtsservice.common.status;

import org.springframework.http.HttpStatus;

import com.iduenduen.mtsservice.common.base.BaseStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseStatus {

    /**
     * Common
     */
    BAD_REQUEST("COMM_400", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    UNAUTHORIZED("COMM_401", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN("COMM_403", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND("COMM_404", HttpStatus.NOT_FOUND, "요청한 자원을 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED("COMM_405", HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 메소드입니다."),
    INTERNAL_SERVER_ERROR("COMM_500", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다."),

    /**
     * Account
     */
    ACCOUNT_NOT_FOUND("ACC_404", HttpStatus.NOT_FOUND, "계좌를 찾을 수 없습니다."),

    /**
     * Order
     */
    INSUFFICIENT_BALANCE("ORD_422", HttpStatus.UNPROCESSABLE_ENTITY, "잔고가 부족합니다."),
    INSUFFICIENT_HOLDING("ORD_422", HttpStatus.UNPROCESSABLE_ENTITY, "보유 수량이 부족합니다."),
    MARKET_CLOSED("ORD_403", HttpStatus.FORBIDDEN, "거래 가능 시간이 아닙니다. (09:00 ~ 15:30)"),
    /**
     * ETF
     */
    ETF_NOT_FOUND("ETF_404", HttpStatus.NOT_FOUND, "존재하지 않는 ETF 종목입니다."),
    LS_API_ERROR("ETF_502", HttpStatus.BAD_GATEWAY, "LS증권 API 호출에 실패했습니다.");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
