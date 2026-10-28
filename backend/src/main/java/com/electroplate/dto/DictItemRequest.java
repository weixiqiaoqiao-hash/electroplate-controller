package com.electroplate.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 字典项新增请求（I9）
 */
@Data
public class DictItemRequest {

    /** 字典类型：CATEGORY 类别 / SPEC 规格 */
    @NotBlank(message = "字典类型不能为空")
    @Pattern(regexp = "CATEGORY|SPEC", message = "字典类型只能为 CATEGORY 或 SPEC")
    private String type;

    /** 字典值 */
    @NotBlank(message = "字典值不能为空")
    @Size(max = 32, message = "字典值长度不能超过 32 字符")
    private String value;
}
