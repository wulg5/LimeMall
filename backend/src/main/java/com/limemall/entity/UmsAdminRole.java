package com.limemall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 管理员-角色关联实体，对应表 ums_admin_role。
 */
@Data
@TableName("ums_admin_role")
public class UmsAdminRole {

    /** 管理员ID */
    @TableId(value = "admin_id", type = IdType.INPUT)
    private Long adminId;

    /** 角色ID */
    private Long roleId;
}
