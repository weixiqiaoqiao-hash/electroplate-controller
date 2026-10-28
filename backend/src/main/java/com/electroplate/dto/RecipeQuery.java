package com.electroplate.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 配方分页查询条件（I1）
 */
@Data
public class RecipeQuery {

    /** 类别，字典值精确匹配，可选 */
    private String category;

    /** 规格，字典值精确匹配，可选 */
    private String spec;

    /** 阻值具体值，按 res_min <= v <= res_max 过滤，可选 */
    private BigDecimal resistance;

    /** 数量具体值，按 qty_min <= v <= qty_max 过滤，可选 */
    private Integer qty;

    /** 状态过滤：ENABLED/DISABLED/ALL，默认 ENABLED */
    private String status = "ENABLED";

    /** 页码，默认 1 */
    private Integer page = 1;

    /** 每页条数，默认 20，上限 100（超出自动截断） */
    private Integer size = 20;
}
