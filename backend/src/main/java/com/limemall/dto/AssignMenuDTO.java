package com.limemall.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色分配菜单权限请求参数。
 */
@Data
public class AssignMenuDTO {

    @NotNull(message = "请选择菜单权限")
    private List<Long> menuIds;
}
