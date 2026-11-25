package com.limemall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体，对应表 oms_order。
 * 状态机：0待付款 → 1待发货 → 2已发货 → 3已完成；0/1 → 4已关闭。
 * 收货信息为下单快照字段。
 */
@Data
@TableName("oms_order")
public class OmsOrder {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单编号 */
    private String orderSn;

    /** 会员ID */
    private Long memberId;

    /** 商品总额 */
    private BigDecimal totalAmount;

    /** 优惠券抵扣金额 */
    private BigDecimal couponAmount;

    /** 运费 */
    private BigDecimal freightAmount;

    /** 应付金额 = 商品总额 - 优惠 + 运费 */
    private BigDecimal payAmount;

    /** 使用的优惠券ID */
    private Long couponId;

    /** 状态：0待付款 1待发货 2已发货 3已完成 4已关闭 */
    private Integer status;

    /** 收货人姓名（快照） */
    private String receiverName;

    /** 收货人电话（快照） */
    private String receiverPhone;

    /** 收货地址（快照） */
    private String receiverAddress;

    /** 买家留言 */
    private String note;

    /** 物流公司 */
    private String logisticsCompany;

    /** 物流单号 */
    private String logisticsSn;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 发货时间 */
    private LocalDateTime deliveryTime;

    /** 确认收货时间 */
    private LocalDateTime receiveTime;

    /** 关闭时间 */
    private LocalDateTime closeTime;

    /** 下单时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
