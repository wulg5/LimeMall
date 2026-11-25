package com.limemall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 首页轮播图实体（后台可配置）。
 */
@Data
@TableName("cms_banner")
public class CmsBanner {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角标文字 */
    private String tag;

    private String title;

    private String subtitle;

    /** 按钮文字 */
    private String ctaText;

    /** 跳转地址 */
    private String linkUrl;

    /** 渐变起始色（如 #0b3d33） */
    private String bgFrom;

    /** 渐变结束色 */
    private String bgTo;

    private String image1;

    private String image2;

    private Integer sort;

    /** 状态：1上架 0下架 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
