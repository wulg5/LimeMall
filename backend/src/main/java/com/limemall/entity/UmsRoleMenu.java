package com.limemall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色-菜单关联实体，对应表 ums_role_menu。
 */
@Data
@TableName("ums_role_menu")
public class UmsRoleMenu {

    /** 角色ID */
    @TableId(value = "role_id", type = IdType.INPUT)
    private Long roleId;

    /** 菜单ID */
    private Long menuId;
}
