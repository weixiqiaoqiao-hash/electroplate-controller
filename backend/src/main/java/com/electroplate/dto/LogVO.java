package com.electroplate.dto;

import com.electroplate.entity.RecipeLog;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志响应视图（I7），before/after 为反序列化后的配方快照，查不出来为 null
 */
@Data
public class LogVO {

    /** 日志主键 */
    private Long id;

    /** 配方ID */
    private Long recipeId;

    /** 配方编号 */
    private String recipeCode;

    /** 动作：CREATE/UPDATE/ENABLE/DISABLE */
    private String action;

    /** 操作人，账号体系上线前留空 */
    private String operator;

    /** 操作时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    /** 变更前配方快照，CREATE 时为 null */
    private JsonNode before;

    /** 变更后配方快照 */
    private JsonNode after;

    /**
     * 实体转视图对象（不含快照反序列化，由 Service 层填充）
     */
    public static LogVO of(RecipeLog l) {
        LogVO vo = new LogVO();
        vo.setId(l.getId());
        vo.setRecipeId(l.getRecipeId());
        vo.setRecipeCode(l.getRecipeCode());
        vo.setAction(l.getAction());
        vo.setOperator(l.getOperator());
        vo.setCreatedAt(l.getCreatedAt());
        return vo;
    }
}
