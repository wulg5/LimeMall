package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统参数配置数据访问层。
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {
}
