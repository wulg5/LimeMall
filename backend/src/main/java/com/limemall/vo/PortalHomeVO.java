package com.limemall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商城前台首页数据（分类树 + 热销 + 新品）。
 */
@Data
public class PortalHomeVO {

    /** 分类树 */
    private java.util.List<CategoryNodeVO> categories;

    /** 热销商品（按销量） */
    private java.util.List<ProductVO> hotProducts;

    /** 新品（按上架时间） */
    private java.util.List<ProductVO> newProducts;
}
