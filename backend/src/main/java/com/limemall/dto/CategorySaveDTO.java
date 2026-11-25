package com.limemall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商品分类新增/编辑请求参数。
 */
@Data
public class CategorySaveDTO {

    /** 编辑时必填 */
    private Long id;

    /** 父级ID，0 表示一级分类 */
    @NotNull(message = "请选择上级分类")
    private Long parentId;

    @NotBlank(message = "请输入分类名称")
    private String name;

    private String icon;

    @NotNull(message = "请输入排序值")
    private Integer sort;

    @NotNull(message = "请选择状态")
    private Integer status;
}
