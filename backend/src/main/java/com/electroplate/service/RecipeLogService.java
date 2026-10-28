package com.electroplate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.electroplate.dto.LogQuery;
import com.electroplate.dto.LogVO;
import com.electroplate.dto.RecipeVO;
import com.electroplate.entity.Recipe;
import com.electroplate.entity.RecipeLog;
import com.electroplate.mapper.RecipeLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 配方操作日志服务：写入留痕（R5）与查询（I7）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeLogService {

    private final RecipeLogMapper recipeLogMapper;
    private final ObjectMapper objectMapper;

    /**
     * 写入操作日志：CREATE 只带 after；UPDATE/ENABLE/DISABLE 带 before+after（变更前完整快照）
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveLog(Recipe recipe, String action, RecipeVO before, RecipeVO after) {
        RecipeLog logEntry = new RecipeLog();
        logEntry.setRecipeId(recipe.getId());
        logEntry.setRecipeCode(recipe.getRecipeCode());
        logEntry.setAction(action);
        // 操作人待账号体系上线后补录，本期留空
        logEntry.setOperator(null);
        logEntry.setBeforeSnapshot(toJson(before));
        logEntry.setAfterSnapshot(toJson(after));
        logEntry.setCreatedAt(java.time.LocalDateTime.now());
        recipeLogMapper.insert(logEntry);
    }

    /**
     * I7 操作日志分页查询：按 created_at 倒序，before/after 反序列化为 JSON 节点
     */
    public Map<String, Object> page(LogQuery query) {
        LambdaQueryWrapper<RecipeLog> wrapper = new LambdaQueryWrapper<RecipeLog>()
                .eq(StringUtils.hasText(query.getRecipeCode()), RecipeLog::getRecipeCode, query.getRecipeCode())
                .eq(StringUtils.hasText(query.getAction()), RecipeLog::getAction, query.getAction())
                .like(StringUtils.hasText(query.getOperator()), RecipeLog::getOperator, query.getOperator())
                .ge(query.getStartTime() != null, RecipeLog::getCreatedAt, query.getStartTime())
                .le(query.getEndTime() != null, RecipeLog::getCreatedAt, query.getEndTime())
                .orderByDesc(RecipeLog::getCreatedAt);

        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int size = query.getSize() == null || query.getSize() < 1 ? 20 : Math.min(query.getSize(), 100);

        Page<RecipeLog> result = recipeLogMapper.selectPage(new Page<RecipeLog>(page, size), wrapper);
        List<LogVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("total", result.getTotal());
        data.put("records", records);
        return data;
    }

    /**
     * 实体转视图：快照字符串反序列化，空或解析失败返回 null
     */
    private LogVO toVO(RecipeLog logEntry) {
        LogVO vo = LogVO.of(logEntry);
        vo.setBefore(parseSnapshot(logEntry.getBeforeSnapshot()));
        vo.setAfter(parseSnapshot(logEntry.getAfterSnapshot()));
        return vo;
    }

    private JsonNode parseSnapshot(String snapshot) {
        if (!StringUtils.hasText(snapshot)) {
            return null;
        }
        try {
            return objectMapper.readTree(snapshot);
        } catch (JsonProcessingException e) {
            log.warn("日志快照解析失败，按 null 处理：{}", e.getMessage());
            return null;
        }
    }

    private String toJson(RecipeVO vo) {
        if (vo == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(vo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("日志快照序列化失败", e);
        }
    }
}
