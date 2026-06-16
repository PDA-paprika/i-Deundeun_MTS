package com.iduenduen.mtsservice.common.exception;



import com.iduenduen.mtsservice.common.base.BaseStatus;

import lombok.Getter;

@Getter
public class GeneralException extends RuntimeException {
    private final BaseStatus errorStatus;

    public GeneralException(BaseStatus errorStatus) {
        super(errorStatus.getMessage());
        this.errorStatus = errorStatus;
    }
}
