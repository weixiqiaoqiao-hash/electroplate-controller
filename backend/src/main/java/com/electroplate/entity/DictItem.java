package com.electroplate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典项实体，对应 dict_item 表（类别/规格两类字典）
 */
@Data
@TableName("dict_item")
public class DictItem {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 字典类型：CATEGORY 类别 / SPEC 规格 */
    private String dictType;

    /** 字典值 */
    private String dictValue;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
