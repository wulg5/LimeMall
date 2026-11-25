package com.limemall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员持有的优惠券视图（我的优惠券）。
 */
@Data
public class MemberCouponVO {

    /** 领取记录ID（下单时作为 couponMemberId 传入） */
    private Long id;

    private Long couponId;

    private String name;

    /** 1满减券 2折扣券 */
    private Integer type;

    private BigDecimal amount;

    private BigDecimal minAmount;

    private LocalDateTime endTime;

    /** 0未使用 1已使用 2已过期 */
    private Integer status;
}
