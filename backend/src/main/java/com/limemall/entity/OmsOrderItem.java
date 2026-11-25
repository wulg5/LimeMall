package com.limemall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细实体，对应表 oms_order_item。
 * 商品名称、图片、规格、单价均为下单时快照，商品后续修改不影响历史订单。
 */
@Data
@TableName("oms_order_item")
public class OmsOrderItem {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 商品ID */
    private Long productId;

    /** 商品名称（快照） */
    private String productName;

    /** 商品主图（快照） */
    private String productPic;

    /** SKU ID */
    private Long skuId;

    /** SKU规格（快照） */
    private String skuSpecs;

    /** 商品单价（快照） */
    private BigDecimal price;

    /** 购买数量 */
    private Integer quantity;

    /** 明细小计 = 单价 × 数量 */
    private BigDecimal itemAmount;

    /** 创建时间 */
    private LocalDateTime createTime;
}
