package com.limemall.vo;

import com.limemall.entity.PmsSku;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品详情视图（SPU + SKU 列表 + 品牌/分类名）。
 */
@Data
public class ProductDetailVO {

    private Long id;

    private String name;

    private String subTitle;

    private Long categoryId;

    private String categoryName;

    private Long brandId;

    private String brandName;

    private String mainPic;

    private String album;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer stock;

    private Integer sales;

    private String unit;

    private String detailHtml;

    private Integer sort;

    private Integer publishStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** SKU 列表 */
    private List<PmsSku> skus;
}
