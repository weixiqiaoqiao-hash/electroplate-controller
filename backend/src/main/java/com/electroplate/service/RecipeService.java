package com.electroplate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.electroplate.common.BusinessException;
import com.electroplate.common.ErrorCode;
import com.electroplate.dto.RecipeQuery;
import com.electroplate.dto.RecipeSaveRequest;
import com.electroplate.dto.RecipeVO;
import com.electroplate.dto.StatusRequest;
import com.electroplate.entity.Recipe;
import com.electroplate.mapper.RecipeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 配方服务：范围重叠校验（R1）、配方编号生成（R4）、事务内写入与日志留痕（R5）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeMapper recipeMapper;
    private final RecipeLogService recipeLogService;
    private final DictService dictService;

    /**
     * I1 分页查询配方：按 updated_at 倒序
     */
    public Map<String, Object> page(RecipeQuery query) {
        Integer statusCode = resolveStatusFilter(query.getStatus());
        LambdaQueryWrapper<Recipe> wrapper = new LambdaQueryWrapper<Recipe>()
                .eq(StringUtils.hasText(query.getCategory()), Recipe::getCategory, query.getCategory())
                .eq(StringUtils.hasText(query.getSpec()), Recipe::getSpec, query.getSpec())
                // 阻值命中区间：res_min <= v <= res_max
                .le(query.getResistance() != null, Recipe::getResMin, query.getResistance())
                .ge(query.getResistance() != null, Recipe::getResMax, query.getResistance())
                // 数量命中区间：qty_min <= v <= qty_max
                .le(query.getQty() != null, Recipe::getQtyMin, query.getQty())
                .ge(query.getQty() != null, Recipe::getQtyMax, query.getQty())
                .eq(statusCode != null, Recipe::getStatus, statusCode)
                .orderByDesc(Recipe::getUpdatedAt);

        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int size = query.getSize() == null || query.getSize() < 1 ? 20 : Math.min(query.getSize(), 100);

        Page<Recipe> result = recipeMapper.selectPage(new Page<Recipe>(page, size), wrapper);
        List<RecipeVO> records = result.getRecords().stream().map(RecipeVO::of)
                .collect(java.util.stream.Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("total", result.getTotal());
        data.put("records", records);
        return data;
    }

    /**
     * I2 配方详情
     */
    public RecipeVO detail(Long id) {
        Recipe recipe = recipeMapper.selectById(id);
        if (recipe == null) {
            throw new BusinessException(ErrorCode.RECIPE_NOT_FOUND);
        }
        return RecipeVO.of(recipe);
    }

    /**
     * I3 新增配方：字典校验 + 重叠校验 + 编号生成 + 写入 + CREATE 日志，同一事务内完成
     */
    @Transactional(rollbackFor = Exception.class)
    public RecipeVO create(RecipeSaveRequest request) {
        validateRangeOrder(request);
        dictService.validateRecipeDict(request.getCategory(), request.getSpec());
        String conflict = findConflict(request.getCategory(), request.getSpec(),
                request.getResMin(), request.getResMax(), request.getQtyMin(), request.getQtyMax(), null);
        if (conflict != null) {
            throw new BusinessException(ErrorCode.RANGE_OVERLAP,
                    "与配方 " + conflict + " 的阻值/数量区间重叠", conflict);
        }

        Recipe recipe = buildEntity(request);
        recipe.setStatus(1);
        recipe.setVersion(0);
        LocalDateTime now = LocalDateTime.now();
        recipe.setCreatedAt(now);
        recipe.setUpdatedAt(now);
        insertWithGeneratedCode(recipe);

        Recipe saved = recipeMapper.selectById(recipe.getId());
        // 新增只写 after 快照
        recipeLogService.saveLog(saved, "CREATE", null, RecipeVO.of(saved));
        return RecipeVO.of(saved);
    }

    /**
     * I4 修改配方：乐观锁拦截并发修改，UPDATE 日志记录变更前后完整快照
     */
    @Transactional(rollbackFor = Exception.class)
    public RecipeVO update(Long id, RecipeSaveRequest request) {
        Recipe before = recipeMapper.selectById(id);
        if (before == null) {
            throw new BusinessException(ErrorCode.RECIPE_NOT_FOUND);
        }
        if (request.getVersion() == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "version 不能为空");
        }
        validateRangeOrder(request);
        dictService.validateRecipeDict(request.getCategory(), request.getSpec());
        // 重叠校验排除自身 id
        String conflict = findConflict(request.getCategory(), request.getSpec(),
                request.getResMin(), request.getResMax(), request.getQtyMin(), request.getQtyMax(), id);
        if (conflict != null) {
            throw new BusinessException(ErrorCode.RANGE_OVERLAP,
                    "与配方 " + conflict + " 的阻值/数量区间重叠", conflict);
        }

        Recipe update = buildEntity(request);
        update.setId(id);
        update.setVersion(request.getVersion());
        update.setUpdatedAt(LocalDateTime.now());
        int rows = recipeMapper.updateById(update);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFY);
        }

        Recipe after = recipeMapper.selectById(id);
        recipeLogService.saveLog(after, "UPDATE", RecipeVO.of(before), RecipeVO.of(after));
        return RecipeVO.of(after);
    }

    /**
     * I5 停用/启用：启用方向重新执行重叠校验；状态未变化直接返回，不写日志
     */
    @Transactional(rollbackFor = Exception.class)
    public RecipeVO changeStatus(Long id, StatusRequest request) {
        Recipe before = recipeMapper.selectById(id);
        if (before == null) {
            throw new BusinessException(ErrorCode.RECIPE_NOT_FOUND);
        }
        boolean enable = "ENABLED".equals(request.getStatus());
        Integer target = enable ? 1 : 0;
        if (target.equals(before.getStatus())) {
            return RecipeVO.of(before);
        }
        // 仅启用方向做重叠校验（停用配方不参与匹配，无需校验）
        if (enable) {
            String conflict = findConflict(before.getCategory(), before.getSpec(),
                    before.getResMin(), before.getResMax(), before.getQtyMin(), before.getQtyMax(), id);
            if (conflict != null) {
                throw new BusinessException(ErrorCode.RANGE_OVERLAP,
                        "与配方 " + conflict + " 的阻值/数量区间重叠", conflict);
            }
        }

        Recipe update = new Recipe();
        update.setId(id);
        update.setStatus(target);
        update.setUpdatedAt(LocalDateTime.now());
        recipeMapper.updateById(update);

        Recipe after = recipeMapper.selectById(id);
        recipeLogService.saveLog(after, enable ? "ENABLE" : "DISABLE",
                RecipeVO.of(before), RecipeVO.of(after));
        return RecipeVO.of(after);
    }

    /**
     * I6 匹配查询：仅启用配方闭区间命中；返回 null 表示无匹配；多条命中为防御分支
     */
    public RecipeVO match(String category, String spec, BigDecimal resistance, Integer qty) {
        List<Recipe> hits = recipeMapper.selectList(new LambdaQueryWrapper<Recipe>()
                .eq(Recipe::getCategory, category)
                .eq(Recipe::getSpec, spec)
                .eq(Recipe::getStatus, 1)
                .le(Recipe::getResMin, resistance)
                .ge(Recipe::getResMax, resistance)
                .le(Recipe::getQtyMin, qty)
                .ge(Recipe::getQtyMax, qty));
        if (hits.isEmpty()) {
            return null;
        }
        if (hits.size() > 1) {
            // R1 规则保证至多一条命中，多条属理论不可能的防御分支
            log.warn("匹配查询命中多条配方：category={}, spec={}, resistance={}, qty={}, 命中编号={}",
                    category, spec, resistance, qty,
                    hits.stream().map(Recipe::getRecipeCode)
                            .collect(java.util.stream.Collectors.joining(",")));
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        return RecipeVO.of(hits.get(0));
    }

    /**
     * 范围重叠判定（PRD 规则 R1）：在同 category+spec 的启用配方中（排除 excludeId），
     * 阻值与数量双区间闭区间交叉即为冲突，返回冲突配方编号；无冲突返回 null
     */
    private String findConflict(String category, String spec, BigDecimal resMin, BigDecimal resMax,
                                Integer qtyMin, Integer qtyMax, Long excludeId) {
        List<Recipe> enabledList = recipeMapper.selectList(new LambdaQueryWrapper<Recipe>()
                .eq(Recipe::getCategory, category)
                .eq(Recipe::getSpec, spec)
                .eq(Recipe::getStatus, 1)
                .ne(excludeId != null, Recipe::getId, excludeId));
        for (Recipe old : enabledList) {
            boolean resOverlap = resMin.compareTo(old.getResMax()) <= 0
                    && old.getResMin().compareTo(resMax) <= 0;
            boolean qtyOverlap = qtyMin <= old.getQtyMax() && old.getQtyMin() <= qtyMax;
            if (resOverlap && qtyOverlap) {
                return old.getRecipeCode();
            }
        }
        return null;
    }

    /**
     * 校验区间下限不大于上限（PRD 规则 R2）
     */
    private void validateRangeOrder(RecipeSaveRequest request) {
        if (request.getResMin().compareTo(request.getResMax()) > 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "阻值下限不能大于上限");
        }
        if (request.getQtyMin() > request.getQtyMax()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "数量下限不能大于上限");
        }
    }

    /**
     * 请求转实体：可空段统一落 0；总时间为三段时间之和（服务端计算，不接受前端提交）
     */
    private Recipe buildEntity(RecipeSaveRequest request) {
        Recipe recipe = new Recipe();
        recipe.setCategory(request.getCategory());
        recipe.setSpec(request.getSpec());
        recipe.setResMin(request.getResMin());
        recipe.setResMax(request.getResMax());
        recipe.setQtyMin(request.getQtyMin());
        recipe.setQtyMax(request.getQtyMax());

        RecipeSaveRequest.SectionParams nickel = request.getNickel();
        recipe.setNiSeg1Current(nickel.getSeg1Current());
        recipe.setNiSeg1Time(nickel.getSeg1Time());
        recipe.setNiSeg2Current(orZero(nickel.getSeg2Current()));
        recipe.setNiSeg2Time(orZero(nickel.getSeg2Time()));
        recipe.setNiSeg3Current(orZero(nickel.getSeg3Current()));
        recipe.setNiSeg3Time(orZero(nickel.getSeg3Time()));
        recipe.setNiTotalTime(totalTime(nickel));
        recipe.setNiProtectCurrent(nickel.getProtectCurrent());
        recipe.setNiAlarmVoltage(nickel.getAlarmVoltage());

        RecipeSaveRequest.SectionParams tin = request.getTin();
        recipe.setSnSeg1Current(tin.getSeg1Current());
        recipe.setSnSeg1Time(tin.getSeg1Time());
        recipe.setSnSeg2Current(orZero(tin.getSeg2Current()));
        recipe.setSnSeg2Time(orZero(tin.getSeg2Time()));
        recipe.setSnSeg3Current(orZero(tin.getSeg3Current()));
        recipe.setSnSeg3Time(orZero(tin.getSeg3Time()));
        recipe.setSnTotalTime(totalTime(tin));
        recipe.setSnProtectCurrent(tin.getProtectCurrent());
        recipe.setSnAlarmVoltage(tin.getAlarmVoltage());
        return recipe;
    }

    /**
     * 总时间 = 三段时间之和（可空段按 0）
     */
    private int totalTime(RecipeSaveRequest.SectionParams params) {
        int t1 = params.getSeg1Time() == null ? 0 : params.getSeg1Time();
        int t2 = params.getSeg2Time() == null ? 0 : params.getSeg2Time();
        int t3 = params.getSeg3Time() == null ? 0 : params.getSeg3Time();
        return t1 + t2 + t3;
    }

    private BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int orZero(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 事务内取当日最大流水 +1 生成编号，uk_recipe_code 唯一键冲突自动重试，上限 3 次
     */
    private void insertWithGeneratedCode(Recipe recipe) {
        for (int attempt = 0; attempt <= 3; attempt++) {
            recipe.setRecipeCode(nextRecipeCode());
            try {
                recipeMapper.insert(recipe);
                return;
            } catch (DuplicateKeyException e) {
                // 编号被并发占用，重新取号重试
                log.warn("配方编号唯一键冲突，自动重试，第 {} 次", attempt + 1);
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "配方编号生成失败，请稍后重试");
    }

    /**
     * 生成下一个配方编号：REC + yyyyMMdd + - 4 位流水
     */
    private String nextRecipeCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "REC" + datePart;
        QueryWrapper<Recipe> wrapper = new QueryWrapper<Recipe>();
        wrapper.select("MAX(recipe_code) AS recipe_code").likeRight("recipe_code", prefix);
        List<Object> objs = recipeMapper.selectObjs(wrapper);
        int next = 1;
        if (objs != null && !objs.isEmpty() && objs.get(0) != null) {
            String maxCode = String.valueOf(objs.get(0));
            next = Integer.parseInt(maxCode.substring(maxCode.length() - 4)) + 1;
        }
        return prefix + "-" + String.format("%04d", next);
    }

    /**
     * 状态过滤参数解析：ENABLED→1、DISABLED→0、ALL→null（不过滤），非法值报参数错误
     */
    private Integer resolveStatusFilter(String status) {
        if (!StringUtils.hasText(status) || "ENABLED".equals(status)) {
            return 1;
        }
        if ("DISABLED".equals(status)) {
            return 0;
        }
        if ("ALL".equals(status)) {
            return null;
        }
        throw new BusinessException(ErrorCode.PARAM_INVALID, "状态只能为 ENABLED、DISABLED 或 ALL");
    }
}
