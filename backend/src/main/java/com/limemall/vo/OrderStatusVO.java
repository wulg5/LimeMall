package com.limemall.vo;

import lombok.Data;

/**
 * 订单状态分布数据项（看板饼图）。
 */
@Data
public class OrderStatusVO {

    /** 订单状态：0待付款 1待发货 2已发货 3已完成 4已关闭 */
    private Integer status;

    /** 订单数 */
    private Long count;
}
