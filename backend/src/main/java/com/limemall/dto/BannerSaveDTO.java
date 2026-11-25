package com.limemall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 轮播图保存参数。
 */
@Data
public class BannerSaveDTO {

    private Long id;

    @Size(max = 32, message = "角标不能超过 32 字")
    private String tag;

    @NotBlank(message = "请填写主标题")
    @Size(max = 100, message = "主标题不能超过 100 字")
    private String title;

    @Size(max = 255, message = "副标题不能超过 255 字")
    private String subtitle;

    @Size(max = 32, message = "按钮文字不能超过 32 字")
    private String ctaText;

    @NotBlank(message = "请填写跳转地址")
    private String linkUrl;

    @NotBlank(message = "请选择渐变起始色")
    private String bgFrom;

    @NotBlank(message = "请选择渐变结束色")
    private String bgTo;

    private String image1;

    private String image2;

    private Integer sort;

    private Integer status;
}
