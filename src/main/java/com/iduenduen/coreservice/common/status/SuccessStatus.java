package com.iduenduen.coreservice.common.status;

import org.springframework.http.HttpStatus;

import com.iduenduen.coreservice.common.base.BaseStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SuccessStatus implements BaseStatus {

    /**
     * Common
     */
    SUCCESS_200("IDEUNDEUN_200", HttpStatus.OK, "성공입니다."),
    SUCCESS_201("IDEUNDEUN_201", HttpStatus.CREATED, "성공입니다."),
    SUCCESS_204("IDEUNDEUN_204", HttpStatus.NO_CONTENT, "성공입니다.");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}