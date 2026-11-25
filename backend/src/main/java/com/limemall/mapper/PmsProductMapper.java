package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.PmsProduct;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品SPU数据访问层。
 * 逻辑删除由 @TableLogic 自动处理：查询自动追加 delete_status = 0。
 */
@Mapper
public interface PmsProductMapper extends BaseMapper<PmsProduct> {
}
