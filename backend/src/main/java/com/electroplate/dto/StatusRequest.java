package com.electroplate.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 配方停用/启用请求（I5）
 */
@Data
public class StatusRequest {

    /** 目标状态：ENABLED 启用 / DISABLED 停用 */
    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "ENABLED|DISABLED", message = "状态只能为 ENABLED 或 DISABLED")
    private String status;
}
