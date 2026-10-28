package com.electroplate.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器：统一转换为 ApiResponse 结构，业务码与 HTTP 状态码同步语义化
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * @RequestBody 上的 Bean Validation 校验失败 → 400 + 40000，message 指明字段
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        return buildFieldErrorResponse(e.getBindingResult().getFieldErrors());
    }

    /**
     * 表单/查询对象绑定与校验失败 → 400 + 40000，message 指明字段
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Object>> handleBindException(BindException e) {
        return buildFieldErrorResponse(e.getBindingResult().getFieldErrors());
    }

    /**
     * 方法参数级校验失败（@Validated + @RequestParam 等）→ 400 + 40000
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException e) {
        String detail = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .collect(Collectors.joining("；"));
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCode.PARAM_INVALID, "参数校验失败：" + detail));
    }

    /**
     * 缺少必填请求参数（如 I6 匹配查询四参数缺一）→ 400 + 40000
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParameter(MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCode.PARAM_INVALID, "参数校验失败：缺少必填参数 " + e.getParameterName()));
    }

    /**
     * 请求参数类型不匹配（如 resistance 传入非数值）→ 400 + 40000
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCode.PARAM_INVALID, "参数校验失败：参数 " + e.getName() + " 类型不合法"));
    }

    /**
     * 业务异常 → 按 ErrorCode 映射 HTTP 状态与业务码
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusiness(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        HttpStatus httpStatus = HttpStatus.resolve(errorCode.getHttpStatus());
        if (httpStatus == null) {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(httpStatus)
                .body(new ApiResponse<Object>(errorCode.getCode(), e.getMessage(), e.getData()));
    }

    /**
     * 兜底异常 → 500 + 50000
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception e) {
        log.error("系统内部异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR));
    }

    /**
     * 拼装字段级校验错误信息，格式：参数校验失败：字段名 提示信息；字段名 提示信息
     */
    private ResponseEntity<ApiResponse<Object>> buildFieldErrorResponse(java.util.List<FieldError> fieldErrors) {
        String detail = fieldErrors.stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .collect(Collectors.joining("；"));
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCode.PARAM_INVALID, "参数校验失败：" + detail));
    }
}
