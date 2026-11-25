package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.SmsCouponMember;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员优惠券数据访问层。
 */
@Mapper
public interface SmsCouponMemberMapper extends BaseMapper<SmsCouponMember> {
}
