package com.electroplate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 配方操作日志实体，对应 recipe_log 表（新增/修改/停用/启用各产生一条）
 */
@Data
@TableName("recipe_log")
public class RecipeLog {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配方ID */
    private Long recipeId;

    /** 配方编号 */
    private String recipeCode;

    /** 动作：CREATE/UPDATE/ENABLE/DISABLE */
    private String action;

    /** 操作人，账号体系上线前留空 */
    private String operator;

    /** 变更前完整 JSON 快照，CREATE 时为空 */
    private String beforeSnapshot;

    /** 变更后完整 JSON 快照 */
    private String afterSnapshot;

    /** 操作时间 */
    private LocalDateTime createdAt;
}
