package com.limemall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商城前台优惠券视图（领取中心）。
 */
@Data
public class PortalCouponVO {

    private Long id;

    private String name;

    /** 1满减券 2折扣券 */
    private Integer type;

    private BigDecimal amount;

    private BigDecimal minAmount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 剩余可领数量 */
    private Integer remaining;

    /** 当前会员是否已领取 */
    private Boolean received;

    /** 积分兑换价（0=不可兑换；100 积分 = 1 元面额） */
    private Integer pointsPrice;
}
