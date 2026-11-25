package com.limemall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品SKU实体，对应表 pms_sku。
 * specs 保存规格 JSON，如 [{"key":"颜色","value":"云雾白"}]。
 */
@Data
@TableName("pms_sku")
public class PmsSku {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属商品ID */
    private Long productId;

    /** SKU编码 */
    private String skuCode;

    /** 规格JSON */
    private String specs;

    /** SKU售价 */
    private BigDecimal price;

    /** SKU库存 */
    private Integer stock;

    /** SKU销量 */
    private Integer sales;

    /** SKU图片 */
    private String pic;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
