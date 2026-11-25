package com.limemall.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品评分聚合。
 */
@Data
public class ProductRatingVO {

    private Long productId;

    /** 评价数量 */
    private Long commentCount;

    /** 平均分（一位小数） */
    private BigDecimal avgRating;
}
