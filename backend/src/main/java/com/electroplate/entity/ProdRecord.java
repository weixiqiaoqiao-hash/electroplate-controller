package com.electroplate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 生产信息记录实体，对应 production_record 表（由扫码上料「发送」动作写入，本模块只读）
 */
@Data
@TableName("production_record")
public class ProdRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 流水号 PRyyyyMMdd-XXXX，服务端生成 */
    private String serialNo;

    /** 作业员 */
    private String operator;

    /** 吊架号 */
    private String rackNo;

    /** 投料槽位名称 */
    private String feedSlot;

    /** 投料时间 */
    private LocalDateTime feedTime;

    /** 进线时间，可空 */
    private LocalDateTime inTime;

    /** 出线时间，可空；空=正在加工 */
    private LocalDateTime outTime;

    /** 批号(左) */
    private String batchNoL;

    /** 料号(左) */
    private String itemNoL;

    /** 规格(左) */
    private String specL;

    /** 阻值(左) */
    private String resistanceL;

    /** 精度(左) */
    private String toleranceL;

    /** 数量(左) */
    private Integer qtyL;

    /** 批号(右) */
    private String batchNoR;

    /** 料号(右) */
    private String itemNoR;

    /** 规格(右) */
    private String specR;

    /** 阻值(右) */
    private String resistanceR;

    /** 精度(右) */
    private String toleranceR;

    /** 数量(右) */
    private Integer qtyR;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 修改时间 */
    private LocalDateTime updatedAt;
}
