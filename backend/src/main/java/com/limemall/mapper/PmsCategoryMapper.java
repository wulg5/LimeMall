package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.PmsCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品分类数据访问层。
 */
@Mapper
public interface PmsCategoryMapper extends BaseMapper<PmsCategory> {
}
