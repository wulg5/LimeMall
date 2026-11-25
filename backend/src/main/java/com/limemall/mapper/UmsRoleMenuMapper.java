package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.UmsRoleMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色-菜单关联数据访问层。
 */
@Mapper
public interface UmsRoleMenuMapper extends BaseMapper<UmsRoleMenu> {
}
