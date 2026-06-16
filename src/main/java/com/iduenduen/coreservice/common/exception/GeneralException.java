package com.iduenduen.coreservice.common.exception;



import com.iduenduen.coreservice.common.base.BaseStatus;

import lombok.Getter;

@Getter
public class GeneralException extends RuntimeException {
    private final BaseStatus errorStatus;

    public GeneralException(BaseStatus errorStatus) {
        super(errorStatus.getMessage());
        this.errorStatus = errorStatus;
    }
}
