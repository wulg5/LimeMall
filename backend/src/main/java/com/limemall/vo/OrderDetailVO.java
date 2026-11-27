package com.limemall.vo;

import com.limemall.entity.OmsOrderItem;
import com.limemall.entity.OmsOrderReturn;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情视图（订单主信息 + 明细 + 会员昵称 + 退款申请）。
 */
@Data
public class OrderDetailVO {

    private Long id;

    private String orderSn;

    private Long memberId;

    private String memberUsername;

    private String memberNickname;

    private BigDecimal totalAmount;

    private BigDecimal couponAmount;

    private BigDecimal freightAmount;

    private BigDecimal payAmount;

    private Long couponId;

    private Integer status;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private String note;

    private String logisticsCompany;

    private String logisticsSn;

    private LocalDateTime payTime;

    private LocalDateTime deliveryTime;

    private LocalDateTime receiveTime;

    private LocalDateTime closeTime;

    private LocalDateTime createTime;

    /** 订单明细 */
    private List<OmsOrderItem> items;

    private List<Long> reviewedProductIds = List.of();

    /** 退款申请（如有） */
    private OmsOrderReturn returnApply;
}
