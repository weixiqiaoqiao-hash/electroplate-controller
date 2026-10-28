package com.electroplate.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 配方新增/修改请求（I3 / I4 共用，修改时额外携带 version 参与乐观锁校验）
 */
@Data
public class RecipeSaveRequest {

    /** 类别（须存在于类别字典） */
    @NotBlank(message = "类别不能为空")
    @Size(max = 32, message = "类别长度不能超过 32 字符")
    private String category;

    /** 规格（须存在于规格字典） */
    @NotBlank(message = "规格不能为空")
    @Size(max = 32, message = "规格长度不能超过 32 字符")
    private String spec;

    /** 阻值下限(Ω) */
    @NotNull(message = "阻值下限不能为空")
    @DecimalMin(value = "0", message = "阻值下限必须大于等于 0")
    private BigDecimal resMin;

    /** 阻值上限(Ω) */
    @NotNull(message = "阻值上限不能为空")
    @DecimalMin(value = "0", message = "阻值上限必须大于等于 0")
    private BigDecimal resMax;

    /** 数量下限(pcs) */
    @NotNull(message = "数量下限不能为空")
    @Min(value = 0, message = "数量下限必须大于等于 0")
    private Integer qtyMin;

    /** 数量上限(pcs) */
    @NotNull(message = "数量上限不能为空")
    @Min(value = 0, message = "数量上限必须大于等于 0")
    private Integer qtyMax;

    /** 镍段参数 */
    @Valid
    @NotNull(message = "镍段参数不能为空")
    private SectionParams nickel;

    /** 锡段参数 */
    @Valid
    @NotNull(message = "锡段参数不能为空")
    private SectionParams tin;

    /** 乐观锁版本号，仅修改时必传（新增时忽略） */
    private Integer version;

    /**
     * 工艺段参数（镍段/锡段各一组，规则相同）
     */
    @Data
    public static class SectionParams {

        /** 一段电流(A) */
        @NotNull(message = "一段电流不能为空")
        @DecimalMin(value = "0", message = "一段电流必须大于等于 0")
        private BigDecimal seg1Current;

        /** 一段时间(Sec) */
        @NotNull(message = "一段时间不能为空")
        @Min(value = 0, message = "一段时间必须大于等于 0")
        private Integer seg1Time;

        /** 二段电流(A)，可空表示该段不使用 */
        @DecimalMin(value = "0", message = "二段电流必须大于等于 0")
        private BigDecimal seg2Current;

        /** 二段时间(Sec)，可空表示该段不使用 */
        @Min(value = 0, message = "二段时间必须大于等于 0")
        private Integer seg2Time;

        /** 三段电流(A)，可空表示该段不使用 */
        @DecimalMin(value = "0", message = "三段电流必须大于等于 0")
        private BigDecimal seg3Current;

        /** 三段时间(Sec)，可空表示该段不使用 */
        @Min(value = 0, message = "三段时间必须大于等于 0")
        private Integer seg3Time;

        /** 保护电流(A) */
        @NotNull(message = "保护电流不能为空")
        @DecimalMin(value = "0", message = "保护电流必须大于等于 0")
        private BigDecimal protectCurrent;

        /** 报警电压(V) */
        @NotNull(message = "报警电压不能为空")
        @DecimalMin(value = "0", message = "报警电压必须大于等于 0")
        private BigDecimal alarmVoltage;
    }
}
