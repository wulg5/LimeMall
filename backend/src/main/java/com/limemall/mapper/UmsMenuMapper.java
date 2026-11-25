package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.UmsMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单权限数据访问层。
 */
@Mapper
public interface UmsMenuMapper extends BaseMapper<UmsMenu> {

    /**
     * 查询指定管理员通过角色获得的全部启用菜单/按钮权限。
     */
    @Select("""
            SELECT DISTINCT m.* FROM ums_menu m
            INNER JOIN ums_role_menu rm ON m.id = rm.menu_id
            INNER JOIN ums_admin_role ar ON rm.role_id = ar.role_id
            WHERE ar.admin_id = #{adminId} AND m.status = 1
            ORDER BY m.sort DESC
            """)
    List<UmsMenu> selectByAdminId(@Param("adminId") Long adminId);
}
