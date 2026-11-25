package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.PmsBrand;
import org.apache.ibatis.annotations.Mapper;

/**
 * 品牌数据访问层。
 */
@Mapper
public interface PmsBrandMapper extends BaseMapper<PmsBrand> {
}
