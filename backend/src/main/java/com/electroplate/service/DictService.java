package com.electroplate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.electroplate.common.BusinessException;
import com.electroplate.common.ErrorCode;
import com.electroplate.dto.DictItemRequest;
import com.electroplate.dto.DictItemVO;
import com.electroplate.entity.DictItem;
import com.electroplate.entity.Recipe;
import com.electroplate.mapper.DictItemMapper;
import com.electroplate.mapper.RecipeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 字典服务：类别/规格字典的查询（I8）、新增（I9）、删除（I10）与配方取值合法性校验（R7）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictService {

    /** 类别字典类型 */
    public static final String TYPE_CATEGORY = "CATEGORY";
    /** 规格字典类型 */
    public static final String TYPE_SPEC = "SPEC";

    private final DictItemMapper dictItemMapper;
    private final RecipeMapper recipeMapper;

    /**
     * I8 字典列表：按创建时间正序（同刻按 id 保证顺序稳定）
     */
    public List<DictItemVO> list(String type) {
        checkType(type);
        List<DictItem> items = dictItemMapper.selectList(new LambdaQueryWrapper<DictItem>()
                .eq(DictItem::getDictType, type)
                .orderByAsc(DictItem::getCreatedAt)
                .orderByAsc(DictItem::getId));
        return items.stream().map(DictItemVO::of).collect(Collectors.toList());
    }

    /**
     * I9 新增字典项：同组唯一由 uk_type_value 兜底，唯一键冲突返回 40000"已存在"
     */
    @Transactional(rollbackFor = Exception.class)
    public DictItemVO create(DictItemRequest request) {
        checkType(request.getType());
        String value = request.getValue() == null ? "" : request.getValue().trim();
        if (value.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "字典值不能为空");
        }
        if (value.length() > 32) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "字典值长度不能超过 32 字符");
        }

        DictItem item = new DictItem();
        item.setDictType(request.getType());
        item.setDictValue(value);
        item.setCreatedAt(LocalDateTime.now());
        try {
            dictItemMapper.insert(item);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAM_INVALID,
                    "字典值已存在：" + request.getType() + " - " + value);
        }
        return DictItemVO.of(item);
    }

    /**
     * I10 删除字典项：被任何配方引用（含停用配方）时返回 40903
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DictItem item = dictItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(ErrorCode.RECIPE_NOT_FOUND, "字典项不存在");
        }
        long referenceCount = countReference(item.getDictType(), item.getDictValue());
        if (referenceCount > 0) {
            throw new BusinessException(ErrorCode.DICT_IN_USE,
                    "字典项已被 " + referenceCount + " 条配方引用，不可删除");
        }
        dictItemMapper.deleteById(id);
    }

    /**
     * 配方新增/修改时校验类别、规格必须存在于对应字典（R7），否则 40000
     */
    public void validateRecipeDict(String category, String spec) {
        if (!exists(TYPE_CATEGORY, category)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID,
                    "类别 " + category + " 未在字典中维护，请先在字典管理中添加");
        }
        if (!exists(TYPE_SPEC, spec)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID,
                    "规格 " + spec + " 未在字典中维护，请先在字典管理中添加");
        }
    }

    /**
     * 统计字典值在 recipe 表中的引用数（含停用配方）
     */
    private long countReference(String dictType, String dictValue) {
        LambdaQueryWrapper<Recipe> wrapper = new LambdaQueryWrapper<Recipe>();
        if (TYPE_CATEGORY.equals(dictType)) {
            wrapper.eq(Recipe::getCategory, dictValue);
        } else {
            wrapper.eq(Recipe::getSpec, dictValue);
        }
        return recipeMapper.selectCount(wrapper);
    }

    private boolean exists(String type, String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return dictItemMapper.selectCount(new LambdaQueryWrapper<DictItem>()
                .eq(DictItem::getDictType, type)
                .eq(DictItem::getDictValue, value)) > 0;
    }

    private void checkType(String type) {
        if (!TYPE_CATEGORY.equals(type) && !TYPE_SPEC.equals(type)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "字典类型只能为 CATEGORY 或 SPEC");
        }
    }
}
