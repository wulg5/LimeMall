package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.OmsOrderReturn;
import org.apache.ibatis.annotations.Mapper;

/**
 * 退款申请数据访问层。
 */
@Mapper
public interface OmsOrderReturnMapper extends BaseMapper<OmsOrderReturn> {
}
