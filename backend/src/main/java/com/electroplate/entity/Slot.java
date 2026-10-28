package com.electroplate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 产线槽位实体，对应 slot 表
 */
@Data
@TableName("slot")
public class Slot {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 槽位名称，全局唯一（含停用） */
    private String slotName;

    /** 槽位类型：0 普通槽 / 1 镀槽 */
    private Integer slotType;

    /** 时间设定值(秒)，所有槽位均需设定 */
    private Integer timeSeconds;

    /** 是否展示时间：true 生产参数设置页显示时间输入框 */
    private Boolean showTime;

    /** 开关状态（仅镀槽有效） */
    private Boolean switchOn;

    /** 工序序号，列表按此升序 */
    private Integer sortNo;

    /** 状态：1 启用，0 停用（软删除） */
    private Integer status;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 修改时间 */
    private LocalDateTime updatedAt;
}
