package com.limemall.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 看板顶部统计卡片数据。
 */
@Data
public class DashboardSummaryVO {

    /** 今日订单数（不含已关闭） */
    private Long todayOrderCount;

    /** 今日成交金额（已支付订单） */
    private BigDecimal todayPayAmount;

    /** 今日新增会员数 */
    private Long todayNewMemberCount;

    /** 在售商品数（已上架） */
    private Long productCount;

    /** 会员总数 */
    private Long memberCount;

    /** 待发货订单数 */
    private Long pendingDeliverCount;

    /** 待审核退款数 */
    private Long pendingReturnCount;
}
