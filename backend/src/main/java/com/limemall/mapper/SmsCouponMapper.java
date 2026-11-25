package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.SmsCoupon;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券数据访问层。
 */
@Mapper
public interface SmsCouponMapper extends BaseMapper<SmsCoupon> {
}
