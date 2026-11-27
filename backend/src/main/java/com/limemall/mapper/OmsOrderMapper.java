package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.OmsOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 订单数据访问层。
 */
@Mapper
public interface OmsOrderMapper extends BaseMapper<OmsOrder> {

    @Select("SELECT * FROM oms_order WHERE id = #{id} FOR UPDATE")
    OmsOrder lockById(Long id);
}
