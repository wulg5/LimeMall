package com.limemall.common;

import lombok.Getter;

/**
 * 业务异常。
 * 服务层校验不通过时抛出，由全局异常处理器统一转换为 Result 响应。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.BAD_REQUEST.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
