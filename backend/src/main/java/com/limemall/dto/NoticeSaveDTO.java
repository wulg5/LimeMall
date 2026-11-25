package com.limemall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 公告保存参数。
 */
@Data
public class NoticeSaveDTO {

    private Long id;

    @NotBlank(message = "请填写公告内容")
    @Size(max = 255, message = "公告内容不能超过 255 字")
    private String content;

    private Integer sort;

    private Integer status;
}
