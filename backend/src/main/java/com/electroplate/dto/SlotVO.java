package com.electroplate.dto;

import com.electroplate.entity.Slot;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 槽位响应视图（J1~J6 统一返回结构）
 */
@Data
public class SlotVO {

    /** 主键 */
    private Long id;

    /** 槽位名称 */
    private String slotName;

    /** 槽位类型：0 普通槽 / 1 镀槽 */
    private Integer slotType;

    /** 时间设定值(秒) */
    private Integer timeSeconds;

    /** 是否展示时间 */
    private Boolean showTime;

    /** 开关状态（仅镀槽有效） */
    private Boolean switchOn;

    /** 工序序号 */
    private Integer sortNo;

    /** 状态：ENABLED 启用 / DISABLED 停用 */
    private String status;

    /** 乐观锁版本号，修改时需回传 */
    private Integer version;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    /** 修改时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;

    /**
     * 实体转视图对象
     */
    public static SlotVO of(Slot s) {
        SlotVO vo = new SlotVO();
        vo.setId(s.getId());
        vo.setSlotName(s.getSlotName());
        vo.setSlotType(s.getSlotType());
        vo.setTimeSeconds(s.getTimeSeconds());
        vo.setShowTime(s.getShowTime());
        vo.setSwitchOn(s.getSwitchOn());
        vo.setSortNo(s.getSortNo());
        vo.setStatus(s.getStatus() != null && s.getStatus() == 1 ? "ENABLED" : "DISABLED");
        vo.setVersion(s.getVersion());
        vo.setCreatedAt(s.getCreatedAt());
        vo.setUpdatedAt(s.getUpdatedAt());
        return vo;
    }
}
