package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.PmsSku;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品SKU数据访问层。
 */
@Mapper
public interface PmsSkuMapper extends BaseMapper<PmsSku> {
}
