package com.limemall.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 优惠券领取记录视图（管理员端）。
 */
@Data
public class CouponRecordVO {

    private Long id;

    private String memberUsername;

    private String memberNickname;

    /** 0未使用 1已使用 2已过期 */
    private Integer status;

    private LocalDateTime receiveTime;

    private LocalDateTime useTime;

    private Long orderId;
}
