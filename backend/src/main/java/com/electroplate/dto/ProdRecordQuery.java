package com.electroplate.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 生产记录查询条件（K1/K2 共用，保证导出与列表所见即所得）
 */
@Data
public class ProdRecordQuery {

    /** 投料时间起（含），格式 yyyy-MM-dd HH:mm:ss，可选 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime feedTimeStart;

    /** 投料时间止（含），格式 yyyy-MM-dd HH:mm:ss，可选 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime feedTimeEnd;

    /** 批号，左右任一包含匹配，可选 */
    private String batchNo;

    /** 吊架号，精确匹配，可选 */
    private String rackNo;

    /** 页码，默认 1（导出忽略） */
    private Integer page = 1;

    /** 每页条数，默认 20，上限 100（导出忽略） */
    private Integer size = 20;
}
