package com.huiyue.common;

import lombok.Getter;

/**
 * 业务异常：由全局异常处理器统一捕获
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
