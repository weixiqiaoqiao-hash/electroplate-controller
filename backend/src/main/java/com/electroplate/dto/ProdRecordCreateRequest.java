package com.electroplate.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 生产记录新增请求（K3，扫码上料「发送」时调用）
 */
@Data
public class ProdRecordCreateRequest {

    /** 作业员姓名，可空（无登录环境手工录入） */
    @Size(max = 32, message = "作业员长度不能超过32")
    private String operator;

    /** 吊架号，必填 */
    @NotBlank(message = "吊架号不能为空")
    @Size(max = 32, message = "吊架号长度不能超过32")
    private String rackNo;

    /** 投料槽位名称，可空 */
    @Size(max = 32, message = "投料槽位长度不能超过32")
    private String feedSlot;

    /** 进线时间，可空，格式 yyyy-MM-dd HH:mm:ss */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime inTime;

    /** 出线时间，可空（空=正在加工），格式 yyyy-MM-dd HH:mm:ss */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime outTime;

    /** 左侧批次 */
    @Size(max = 64, message = "批号(左)长度不能超过64")
    private String batchNoL;

    @Size(max = 64, message = "料号(左)长度不能超过64")
    private String itemNoL;

    @Size(max = 32, message = "规格(左)长度不能超过32")
    private String specL;

    @Size(max = 32, message = "阻值(左)长度不能超过32")
    private String resistanceL;

    @Size(max = 32, message = "精度(左)长度不能超过32")
    private String toleranceL;

    private Integer qtyL;

    /** 右侧批次 */
    @Size(max = 64, message = "批号(右)长度不能超过64")
    private String batchNoR;

    @Size(max = 64, message = "料号(右)长度不能超过64")
    private String itemNoR;

    @Size(max = 32, message = "规格(右)长度不能超过32")
    private String specR;

    @Size(max = 32, message = "阻值(右)长度不能超过32")
    private String resistanceR;

    @Size(max = 32, message = "精度(右)长度不能超过32")
    private String toleranceR;

    private Integer qtyR;
}
