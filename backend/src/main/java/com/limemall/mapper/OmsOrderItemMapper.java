package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.OmsOrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单明细数据访问层。
 */
@Mapper
public interface OmsOrderItemMapper extends BaseMapper<OmsOrderItem> {
}
