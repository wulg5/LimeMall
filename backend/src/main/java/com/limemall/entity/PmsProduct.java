package com.limemall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品SPU实体，对应表 pms_product。
 * stock/price 为 SKU 的汇总冗余字段；delete_status 为逻辑删除标记。
 */
@Data
@TableName("pms_product")
public class PmsProduct {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品名称 */
    private String name;

    /** 副标题/卖点 */
    private String subTitle;

    /** 分类ID（二级分类） */
    private Long categoryId;

    /** 品牌ID */
    private Long brandId;

    /** 主图URL */
    private String mainPic;

    /** 画册图片URL（逗号分隔） */
    private String album;

    /** 展示价（取SKU最低价） */
    private BigDecimal price;

    /** 生效秒杀价，仅价格排序查询返回。 */
    @TableField(exist = false)
    private BigDecimal flashPrice;

    /** 划线原价 */
    private BigDecimal originalPrice;

    /** 总库存（SKU汇总） */
    private Integer stock;

    /** 累计销量 */
    private Integer sales;

    /** 单位 */
    private String unit;

    /** 商品详情（富文本） */
    private String detailHtml;

    /** 排序 */
    private Integer sort;

    /** 上架状态：1上架 0下架 */
    private Integer publishStatus;

    /** 逻辑删除：1已删除 0未删除 */
    @TableLogic
    private Integer deleteStatus;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
