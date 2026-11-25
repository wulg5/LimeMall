package com.limemall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券实体，对应表 sms_coupon。
 * type：1满减券（amount 为减免金额） 2折扣券（amount 为折扣，如 9.5 表示 95 折）。
 */
@Data
@TableName("sms_coupon")
public class SmsCoupon {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 优惠券名称 */
    private String name;

    /** 类型：1满减券 2折扣券 */
    private Integer type;

    /** 满减金额 / 折扣（9.5=95折） */
    private BigDecimal amount;

    /** 使用门槛（订单满X元可用） */
    private BigDecimal minAmount;

    /** 发行总量 */
    private Integer totalCount;

    /** 已领取数量 */
    private Integer receiveCount;

    /** 已使用数量 */
    private Integer usedCount;

    /** 每人限领数量 */
    private Integer perLimit;

    /** 有效期开始 */
    private LocalDateTime startTime;

    /** 有效期结束 */
    private LocalDateTime endTime;

    /** 状态：1进行中 0已停止 */
    private Integer status;

    /** 积分兑换价（0=不参与积分兑换；100 积分 = 1 元面额） */
    private Integer pointsPrice;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
