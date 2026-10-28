package com.electroplate.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 镀槽开关切换请求（J6）
 */
@Data
public class SwitchRequest {

    /** 目标开关状态 */
    @NotNull(message = "开关状态不能为空")
    private Boolean switchOn;
}
