package com.limemall.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 热销商品排行数据项。
 */
@Data
public class TopProductVO {

    /** 商品名称 */
    private String name;

    /** 销量（件） */
    private Long quantity;

    /** 销售额 */
    private BigDecimal amount;
}
