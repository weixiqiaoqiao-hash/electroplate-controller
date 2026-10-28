package com.electroplate.dto;

import com.electroplate.entity.ProdRecord;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 生产信息记录视图对象（列表与导出同序同字段）
 */
@Data
public class ProdRecordVO {

    private Long id;

    /** 流水号 */
    private String serialNo;

    /** 作业员 */
    private String operator;

    /** 吊架号 */
    private String rackNo;

    /** 投料槽位 */
    private String feedSlot;

    /** 投料时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime feedTime;

    /** 进线时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inTime;

    /** 出线时间，空=正在加工 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
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

    /** 状态：PROCESSING 正在加工 / DONE 已完成（按出线时间计算，不落库） */
    private String status;

    public static ProdRecordVO of(ProdRecord r) {
        ProdRecordVO vo = new ProdRecordVO();
        vo.setId(r.getId());
        vo.setSerialNo(r.getSerialNo());
        vo.setOperator(r.getOperator());
        vo.setRackNo(r.getRackNo());
        vo.setFeedSlot(r.getFeedSlot());
        vo.setFeedTime(r.getFeedTime());
        vo.setInTime(r.getInTime());
        vo.setOutTime(r.getOutTime());
        vo.setBatchNoL(r.getBatchNoL());
        vo.setItemNoL(r.getItemNoL());
        vo.setSpecL(r.getSpecL());
        vo.setResistanceL(r.getResistanceL());
        vo.setToleranceL(r.getToleranceL());
        vo.setQtyL(r.getQtyL());
        vo.setBatchNoR(r.getBatchNoR());
        vo.setItemNoR(r.getItemNoR());
        vo.setSpecR(r.getSpecR());
        vo.setResistanceR(r.getResistanceR());
        vo.setToleranceR(r.getToleranceR());
        vo.setQtyR(r.getQtyR());
        vo.setStatus(r.getOutTime() == null ? "PROCESSING" : "DONE");
        return vo;
    }
}
