package com.limemall.vo;

import lombok.Data;

import java.util.List;

/**
 * 当前登录管理员信息（含角色、权限标识与菜单树）。
 * 前端据此渲染侧边栏菜单并做按钮级权限控制（v-perm 指令）。
 */
@Data
public class AdminInfoVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    /** 角色名称列表 */
    private List<String> roles;

    /** 权限标识列表（ums_menu.perm），按钮级控制使用 */
    private List<String> perms;

    /** 菜单树（type 0目录/1菜单） */
    private List<MenuNodeVO> menus;
}
