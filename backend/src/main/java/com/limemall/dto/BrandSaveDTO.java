package com.limemall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 品牌新增/编辑请求参数。
 */
@Data
public class BrandSaveDTO {

    /** 编辑时必填 */
    private Long id;

    @NotBlank(message = "请输入品牌名称")
    private String name;

    private String firstLetter;

    private String logo;

    private String description;

    @NotNull(message = "请输入排序值")
    private Integer sort;

    @NotNull(message = "请选择状态")
    private Integer status;
}
