package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.UmsAdmin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员数据访问层。
 */
@Mapper
public interface UmsAdminMapper extends BaseMapper<UmsAdmin> {
}
