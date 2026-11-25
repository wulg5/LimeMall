package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.UmsPointsLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 积分批次账本数据访问层。
 */
@Mapper
public interface UmsPointsLogMapper extends BaseMapper<UmsPointsLog> {
}
