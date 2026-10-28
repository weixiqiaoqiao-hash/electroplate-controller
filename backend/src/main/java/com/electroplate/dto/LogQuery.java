package com.electroplate.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 操作日志分页查询条件（I7）
 */
@Data
public class LogQuery {

    /** 配方编号，精确匹配，可选 */
    private String recipeCode;

    /** 动作：CREATE/UPDATE/ENABLE/DISABLE，可选 */
    private String action;

    /** 操作人，模糊匹配，可选 */
    private String operator;

    /** 操作时间起（含），格式 yyyy-MM-dd HH:mm:ss，可选 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 操作时间止（含），格式 yyyy-MM-dd HH:mm:ss，可选 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /** 页码，默认 1 */
    private Integer page = 1;

    /** 每页条数，默认 20，上限 100（超出自动截断） */
    private Integer size = 20;
}
