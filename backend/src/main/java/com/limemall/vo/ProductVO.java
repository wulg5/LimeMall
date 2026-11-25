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

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
