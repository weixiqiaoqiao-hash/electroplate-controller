package com.electroplate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.electroplate.common.BusinessException;
import com.electroplate.common.ErrorCode;
import com.electroplate.dto.SlotQuery;
import com.electroplate.dto.SlotSaveRequest;
import com.electroplate.dto.SlotTimeBatchRequest;
import com.electroplate.dto.SlotVO;
import com.electroplate.entity.Slot;
import com.electroplate.mapper.SlotMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 槽位服务：名称唯一（R1）、工序排序（R2）、时间批量乐观锁保存（R7）、开关即时切换
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlotService {

    /** 槽位类型：镀槽 */
    private static final int TYPE_PLATING = 1;

    private final SlotMapper slotMapper;

    /**
     * J1 槽位列表：按 sort_no ASC, id ASC 全量返回（槽位数量少，不分页）
     */
    public List<SlotVO> list(SlotQuery query) {
        Integer statusCode = resolveStatusFilter(query == null ? null : query.getStatus());
        List<Slot> slots = slotMapper.selectList(new LambdaQueryWrapper<Slot>()
                .eq(statusCode != null, Slot::getStatus, statusCode)
                .orderByAsc(Slot::getSortNo)
                .orderByAsc(Slot::getId));
        return slots.stream().map(SlotVO::of).collect(Collectors.toList());
    }

    /**
     * J2 新增槽位：默认启用、version=0；普通槽开关强制关，镀槽开关缺省为开
     */
    public SlotVO create(SlotSaveRequest request) {
        checkNameUnique(request.getSlotName(), null);
        checkSortNoUnique(request.getSortNo(), null);
        Slot slot = buildEntity(request);
        if (slot.getSwitchOn() == null) {
            slot.setSwitchOn(true);
        }
        if (slot.getSlotType() != TYPE_PLATING) {
            slot.setSwitchOn(false);
        }
        slot.setStatus(1);
        slot.setVersion(0);
        LocalDateTime now = LocalDateTime.now();
        slot.setCreatedAt(now);
        slot.setUpdatedAt(now);
        try {
            slotMapper.insert(slot);
        } catch (DuplicateKeyException e) {
            // uk_slot_name 兜底并发新增
            throw new BusinessException(ErrorCode.SLOT_NAME_DUP);
        }
        return SlotVO.of(slotMapper.selectById(slot.getId()));
    }

    /**
     * J3 修改槽位配置：乐观锁拦截并发修改
     */
    public SlotVO update(Long id, SlotSaveRequest request) {
        Slot before = slotMapper.selectById(id);
        if (before == null) {
            throw new BusinessException(ErrorCode.SLOT_NOT_FOUND);
        }
        if (request.getVersion() == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "version 不能为空");
        }
        checkNameUnique(request.getSlotName(), id);
        checkSortNoUnique(request.getSortNo(), id);

        Slot update = buildEntity(request);
        if (update.getSwitchOn() == null) {
            update.setSwitchOn(true);
        }
        if (update.getSlotType() != TYPE_PLATING) {
            update.setSwitchOn(false);
        }
        update.setId(id);
        update.setVersion(request.getVersion());
        update.setUpdatedAt(LocalDateTime.now());
        int rows = slotMapper.updateById(update);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFY);
        }
        return SlotVO.of(slotMapper.selectById(id));
    }

    /**
     * J4 删除槽位：物理删除，删除后名称与工序序号即释放
     */
    public void delete(Long id) {
        Slot before = slotMapper.selectById(id);
        if (before == null) {
            throw new BusinessException(ErrorCode.SLOT_NOT_FOUND);
        }
        slotMapper.deleteById(id);
    }

    /**
     * J5 批量保存时间：单事务逐项乐观锁更新，任一项失败整批回滚（PRD R7）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTimes(SlotTimeBatchRequest request) {
        List<SlotTimeBatchRequest.Item> items = request.getItems();
        // 先整体核对存在性，缺失项直接报 40402 并指明槽位
        List<Long> ids = items.stream().map(SlotTimeBatchRequest.Item::getId).collect(Collectors.toList());
        Map<Long, Slot> existMap = slotMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Slot::getId, s -> s));
        for (SlotTimeBatchRequest.Item item : items) {
            if (!existMap.containsKey(item.getId())) {
                throw new BusinessException(ErrorCode.SLOT_NOT_FOUND, "槽位 id=" + item.getId() + " 不存在");
            }
        }
        // 逐项带 version 更新，行数为 0 说明版本冲突
        for (SlotTimeBatchRequest.Item item : items) {
            Slot update = new Slot();
            update.setId(item.getId());
            update.setTimeSeconds(item.getTimeSeconds());
            update.setVersion(item.getVersion());
            update.setUpdatedAt(LocalDateTime.now());
            int rows = slotMapper.updateById(update);
            if (rows == 0) {
                Slot current = existMap.get(item.getId());
                String name = current == null ? String.valueOf(item.getId()) : current.getSlotName();
                throw new BusinessException(ErrorCode.CONCURRENT_MODIFY, "槽位「" + name + "」已被他人修改，请刷新后重试");
            }
        }
    }

    /**
     * J6 开关即时切换：仅镀槽允许；后发覆盖，不走乐观锁（高频快速操作）
     */
    public SlotVO switchSlot(Long id, boolean switchOn) {
        Slot slot = slotMapper.selectById(id);
        if (slot == null) {
            throw new BusinessException(ErrorCode.SLOT_NOT_FOUND);
        }
        if (slot.getSlotType() == null || slot.getSlotType() != TYPE_PLATING) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "只有镀槽才能切换开关");
        }
        Slot update = new Slot();
        update.setId(id);
        update.setSwitchOn(switchOn);
        update.setUpdatedAt(LocalDateTime.now());
        slotMapper.updateById(update);
        return SlotVO.of(slotMapper.selectById(id));
    }

    /**
     * 名称唯一校验（PRD R1，全表），excludeId 用于修改时排除自身
     */
    private void checkNameUnique(String slotName, Long excludeId) {
        Long count = slotMapper.selectCount(new LambdaQueryWrapper<Slot>()
                .eq(Slot::getSlotName, slotName)
                .ne(excludeId != null, Slot::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.SLOT_NAME_DUP);
        }
    }

    /**
     * 工序序号唯一校验（PRD R2，全表）
     */
    private void checkSortNoUnique(Integer sortNo, Long excludeId) {
        Long count = slotMapper.selectCount(new LambdaQueryWrapper<Slot>()
                .eq(Slot::getSortNo, sortNo)
                .ne(excludeId != null, Slot::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.SLOT_SORT_NO_DUP);
        }
    }

    /**
     * 请求转实体（不含 id/version/状态/时间戳）
     */
    private Slot buildEntity(SlotSaveRequest request) {
        Slot slot = new Slot();
        slot.setSlotName(request.getSlotName());
        slot.setSlotType(request.getSlotType());
        slot.setTimeSeconds(request.getTimeSeconds());
        slot.setShowTime(request.getShowTime());
        slot.setSwitchOn(request.getSwitchOn());
        slot.setSortNo(request.getSortNo());
        return slot;
    }

    /**
     * 状态过滤参数解析：空/ENABLED→1、DISABLED→0、ALL→null，非法值报参数错误
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
