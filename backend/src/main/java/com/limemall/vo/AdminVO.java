package com.limemall.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员列表项视图（脱敏，不含密码；含角色信息）。
 */
@Data
public class AdminVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String email;

    private String phone;

    private Integer status;

    /** 角色ID列表（编辑回显） */
    private List<Long> roleIds;

    /** 角色名称（列表展示） */
    private String roleNames;

    private LocalDateTime createTime;
}
