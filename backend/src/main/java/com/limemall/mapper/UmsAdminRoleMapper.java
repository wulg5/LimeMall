package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.UmsAdminRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员-角色关联数据访问层。
 */
@Mapper
public interface UmsAdminRoleMapper extends BaseMapper<UmsAdminRole> {
}
