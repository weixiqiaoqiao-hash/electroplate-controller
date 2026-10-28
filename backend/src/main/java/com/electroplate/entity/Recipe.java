package com.electroplate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 电镀配方实体，对应 recipe 表（配方主表只存当前值，历史数值由 recipe_log 快照承载）
 */
@Data
@TableName("recipe")
public class Recipe {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配方编号 RECyyyyMMdd-XXXX，系统生成后不变 */
    private String recipeCode;

    /** 类别，如 RC/AS/HP，取自类别字典 */
    private String category;

    /** 规格，如 0603/1210，取自规格字典 */
    private String spec;

    /** 阻值下限(Ω) */
    private BigDecimal resMin;

    /** 阻值上限(Ω) */
    private BigDecimal resMax;

    /** 数量下限(pcs) */
    private Integer qtyMin;

    /** 数量上限(pcs) */
    private Integer qtyMax;

    /** 镍段一段电流(A) */
    private BigDecimal niSeg1Current;

    /** 镍段一段时间(Sec) */
    private Integer niSeg1Time;

    /** 镍段二段电流(A)，0 表示该段不使用 */
    private BigDecimal niSeg2Current;

    /** 镍段二段时间(Sec) */
    private Integer niSeg2Time;

    /** 镍段三段电流(A) */
    private BigDecimal niSeg3Current;

    /** 镍段三段时间(Sec) */
    private Integer niSeg3Time;

    /** 镍段总时间(Sec)，服务端按三段时间之和计算 */
    private Integer niTotalTime;

    /** 镍段保护电流(A) */
    private BigDecimal niProtectCurrent;

    /** 镍段报警电压(V) */
    private BigDecimal niAlarmVoltage;

    /** 锡段一段电流(A) */
    private BigDecimal snSeg1Current;

    /** 锡段一段时间(Sec) */
    private Integer snSeg1Time;

    /** 锡段二段电流(A)，0 表示该段不使用 */
    private BigDecimal snSeg2Current;

    /** 锡段二段时间(Sec) */
    private Integer snSeg2Time;

    /** 锡段三段电流(A) */
    private BigDecimal snSeg3Current;

    /** 锡段三段时间(Sec) */
    private Integer snSeg3Time;

    /** 锡段总时间(Sec)，服务端按三段时间之和计算 */
    private Integer snTotalTime;

    /** 锡段保护电流(A) */
    private BigDecimal snProtectCurrent;

    /** 锡段报警电压(V) */
    private BigDecimal snAlarmVoltage;

    /** 状态：1 启用，0 停用（软删除） */
    private Integer status;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    /** 创建人，账号体系上线前留空 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 修改人，账号体系上线前留空 */
    private String updatedBy;

    /** 修改时间 */
    private LocalDateTime updatedAt;
}
