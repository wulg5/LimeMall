package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.SysAdminLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作审计日志数据访问层。
 */
@Mapper
public interface SysAdminLogMapper extends BaseMapper<SysAdminLog> {
}
