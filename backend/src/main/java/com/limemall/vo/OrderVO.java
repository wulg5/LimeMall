package com.limemall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单列表项视图（含会员昵称与商品概要）。
 */
@Data
public class OrderVO {

    private Long id;

    private String orderSn;

    private Long memberId;

    private String memberNickname;

    private BigDecimal totalAmount;

    private BigDecimal couponAmount;

    private BigDecimal freightAmount;

    private BigDecimal payAmount;

    /** 状态：0待付款 1待发货 2已发货 3已完成 4已关闭 */
    private Integer status;

    /** 商品件数 */
    private Integer itemCount;

    /** 首个商品名称（列表摘要展示） */
    private String firstItemName;

    /** 首个商品图片（列表摘要展示） */
    private String firstItemPic;

    /** 是否已评价（已完成订单展示评价入口用） */
    private Boolean commented;

    private String receiverName;

    private String receiverPhone;

    private LocalDateTime createTime;
}
