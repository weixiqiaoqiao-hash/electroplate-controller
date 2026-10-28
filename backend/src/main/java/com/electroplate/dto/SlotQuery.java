package com.electroplate.dto;

import lombok.Data;

/**
 * 槽位列表查询参数（J1），全量不分页
 */
@Data
public class SlotQuery {

    /** 状态过滤：ENABLED / DISABLED / ALL，默认 ENABLED */
    private String status;
}
