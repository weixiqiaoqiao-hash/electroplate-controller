package com.electroplate.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应包：{ "code": 0, "message": "success", "data": ... }
 *
 * @param <T> 业务数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /** 业务码，0 表示成功 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 业务数据，失败时可能为 null 或冲突信息 */
    private T data;

    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<T>(0, "success", data);
    }

    /**
     * 失败响应（使用错误码默认提示语）
     */
    public static ApiResponse<Object> error(ErrorCode errorCode) {
        return new ApiResponse<Object>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 失败响应（自定义提示语）
     */
    public static ApiResponse<Object> error(ErrorCode errorCode, String message) {
        return new ApiResponse<Object>(errorCode.getCode(), message, null);
    }
}
