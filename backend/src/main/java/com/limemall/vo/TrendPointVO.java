package com.limemall.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 销售趋势图数据点（按天）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendPointVO {

    /** 日期 yyyy-MM-dd */
    private String date;

    /** 订单数 */
    private Long orderCount;

    /** 成交金额 */
    private BigDecimal payAmount;
}
