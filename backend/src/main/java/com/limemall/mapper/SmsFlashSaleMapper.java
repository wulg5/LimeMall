package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.SmsFlashSale;
import org.apache.ibatis.annotations.Mapper;

/**
 * 秒杀活动数据访问层。
 */
@Mapper
public interface SmsFlashSaleMapper extends BaseMapper<SmsFlashSale> {
}
