package com.electroplate.dto;

import com.electroplate.entity.DictItem;
import lombok.Data;

/**
 * 字典项响应视图（I8/I9）
 */
@Data
public class DictItemVO {

    /** 字典项主键 */
    private Long id;

    /** 字典值 */
    private String value;

    /**
     * 实体转视图对象
     */
    public static DictItemVO of(DictItem item) {
        DictItemVO vo = new DictItemVO();
        vo.setId(item.getId());
        vo.setValue(item.getDictValue());
        return vo;
    }
}
