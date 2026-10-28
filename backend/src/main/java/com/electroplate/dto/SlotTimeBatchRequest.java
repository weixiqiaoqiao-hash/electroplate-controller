package com.electroplate.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量保存时间请求（J5，时间设定模式的统一保存按钮）
 */
@Data
public class SlotTimeBatchRequest {

    /** 被修改的槽位时间列表 */
    @Valid
    @NotEmpty(message = "时间修改列表不能为空")
    private List<Item> items;

    /**
     * 单个槽位的时间修改项
     */
    @Data
    public static class Item {

        /** 槽位 id */
        @NotNull(message = "槽位 id 不能为空")
        private Long id;

        /** 时间设定值(秒) */
        @NotNull(message = "时间设定值不能为空")
        @Min(value = 0, message = "时间设定值必须大于等于 0")
        private Integer timeSeconds;

        /** 乐观锁版本号 */
        @NotNull(message = "version 不能为空")
        @Min(value = 0, message = "version 不合法")
        private Integer version;
    }
}
