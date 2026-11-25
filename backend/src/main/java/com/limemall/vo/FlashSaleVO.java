package com.limemall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 前台秒杀活动展示对象。
 */
@Data
public class FlashSaleVO {

    private Long id;

    private String name;

    private Long productId;

    private String productName;

    private String mainPic;

    private Long skuId;

    /** SKU 规格 JSON */
    private String skuSpecs;

    /** 原价（SKU 售价） */
    private BigDecimal originalPrice;

    /** 秒杀价 */
    private BigDecimal flashPrice;

    private LocalDateTime endTime;
}
