package com.electroplate.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 槽位新增/修改请求（J2 / J3 共用，修改时额外携带 version 参与乐观锁校验）
 */
@Data
public class SlotSaveRequest {

    /** 槽位名称，全局唯一（含停用） */
    @NotBlank(message = "槽位名称不能为空")
    @Size(max = 32, message = "槽位名称长度不能超过 32 字符")
    private String slotName;

    /** 槽位类型：0 普通槽 / 1 镀槽 */
    @NotNull(message = "槽位类型不能为空")
    @Min(value = 0, message = "槽位类型只能为 0 或 1")
    @Max(value = 1, message = "槽位类型只能为 0 或 1")
    private Integer slotType;

    /** 时间设定值(秒)，所有槽位（含不展示时间的）均需设定 */
    @NotNull(message = "时间设定值不能为空")
    @Min(value = 0, message = "时间设定值必须大于等于 0")
    private Integer timeSeconds;

    /** 是否展示时间：false=生产参数设置页不显示时间输入框 */
    @NotNull(message = "是否展示时间不能为空")
    private Boolean showTime;

    /** 工序序号，≥1 */
    @NotNull(message = "工序序号不能为空")
    @Min(value = 1, message = "工序序号必须大于等于 1")
    private Integer sortNo;

    /** 开关初始状态，仅镀槽有意义；为空时镀槽默认开，普通槽强制关 */
    private Boolean switchOn;

    /** 乐观锁版本号，仅修改时必传（新增时忽略） */
    private Integer version;
}
