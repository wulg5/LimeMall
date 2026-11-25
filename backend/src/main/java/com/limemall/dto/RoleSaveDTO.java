package com.limemall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色新增/编辑请求参数。
 */
@Data
public class RoleSaveDTO {

    /** 编辑时必填 */
    private Long id;

    @NotBlank(message = "请输入角色名称")
    private String name;

    private String description;

    @NotNull(message = "请输入排序值")
    private Integer sort;

    @NotNull(message = "请选择状态")
    private Integer status;

    /** 分配菜单权限时使用（可选） */
    private List<Long> menuIds;
}
