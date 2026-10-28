package com.electroplate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.electroplate.entity.ProdRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 生产信息记录 Mapper
 */
@Mapper
public interface ProdRecordMapper extends BaseMapper<ProdRecord> {
}
