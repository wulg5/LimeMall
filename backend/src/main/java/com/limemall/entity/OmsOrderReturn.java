package com.limemall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款申请实体，对应表 oms_order_return。
 * 审核状态：0待审核 1已通过 2已拒绝；审核通过后订单关闭、库存回补。
 */
@Data
@TableName("oms_order_return")
public class OmsOrderReturn {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 订单编号 */
    private String orderSn;

    /** 会员ID */
    private Long memberId;

    /** 退款原因 */
    private String reason;

    /** 问题描述 */
    private String description;

    /** 退款金额 */
    private BigDecimal amount;

    /** 审核状态：0待审核 1已通过 2已拒绝 */
    private Integer status;

    /** 审核备注 */
    private String handleNote;

    /** 审核时间 */
    private LocalDateTime handleTime;

    /** 申请时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
