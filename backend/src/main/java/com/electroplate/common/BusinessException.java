package com.electroplate.common;

import lombok.Getter;

/**
 * 业务异常：携带错误码、提示信息与可选的业务数据（如冲突配方编号）
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 错误码 */
    private final ErrorCode errorCode;

    /** 附加业务数据（如 40901 时的冲突配方编号） */
    private final transient Object data;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage(), null);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public BusinessException(ErrorCode errorCode, String message, Object data) {
        super(message);
        this.errorCode = errorCode;
        this.data = data;
    }
}
