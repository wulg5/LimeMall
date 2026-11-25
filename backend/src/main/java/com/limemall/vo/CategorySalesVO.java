package com.limemall.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 分类销售占比数据项。
 */
@Data
public class CategorySalesVO {

    /** 一级分类名称 */
    private String name;

    /** 销售额 */
    private BigDecimal amount;
}
