package com.limemall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员优惠券实体，对应表 sms_coupon_member（领取记录）。
 * 状态：0未使用 1已使用 2已过期；order_id 记录使用订单，支撑核销与退券追溯。
 */
@Data
@TableName("sms_coupon_member")
public class SmsCouponMember {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 优惠券ID */
    private Long couponId;

    /** 会员ID */
    private Long memberId;

    /** 状态：0未使用 1已使用 2已过期 */
    private Integer status;

    /** 使用订单ID */
    private Long orderId;

    /** 领取时间 */
    private LocalDateTime receiveTime;

    /** 使用时间 */
    private LocalDateTime useTime;
}
