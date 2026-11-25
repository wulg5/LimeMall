package com.limemall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limemall.entity.UmsRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色数据访问层。
 */
@Mapper
public interface UmsRoleMapper extends BaseMapper<UmsRole> {

    /**
     * 查询指定管理员的全部角色。
     */
    @Select("""
            SELECT r.* FROM ums_role r
            INNER JOIN ums_admin_role ar ON r.id = ar.role_id
            WHERE ar.admin_id = #{adminId} AND r.status = 1
            ORDER BY r.sort DESC
            """)
    List<UmsRole> selectByAdminId(@Param("adminId") Long adminId);
}
