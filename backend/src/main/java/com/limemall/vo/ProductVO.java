package com.limemall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品列表项视图（含品牌名、分类名）。
 */
@Data
public class ProductVO {

    private Long id;

    private String name;

    private String subTitle;

    private Long categoryId;

    private String categoryName;

    private Long brandId;

    private String brandName;

    private String mainPic;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer stock;

    private Integer sales;

    private String unit;

    private Integer sort;

    /** 上架状态：1上架 0下架 */
    private Integer publishStatus;

    /** 平均评分（一位小数，无评价时为空） */
    private java.math.BigDecimal avgRating;

    /** 评价数量 */
    private Long commentCount;

    /** 生效中的秒杀最低价（无秒杀时为空） */
    private java.math.BigDecimal flashPrice;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
