package com.limemall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 菜单权限实体，对应表 ums_menu。
 * type：0目录 1菜单 2按钮；perm 为接口权限标识（如 pms:product:create）。
 */
@Data
@TableName("ums_menu")
public class UmsMenu {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父级ID（0为顶级） */
    private Long parentId;

    /** 名称 */
    private String name;

    /** 前端路由地址 */
    private String path;

    /** 图标标识 */
    private String icon;

    /** 权限标识 */
    private String perm;

    /** 类型：0目录 1菜单 2按钮 */
    private Integer type;

    /** 排序（同级内降序） */
    private Integer sort;

    /** 状态：1启用 0禁用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
