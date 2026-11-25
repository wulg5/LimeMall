package com.limemall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 限时秒杀实体（SKU 级秒杀价，服务端下单时强制生效）。
 */
@Data
@TableName("sms_flash_sale")
public class SmsFlashSale {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 活动名称 */
    private String name;

    /** 参与秒杀的 SKU（唯一） */
    private Long skuId;

    /** 秒杀价 */
    private BigDecimal flashPrice;

    /** 每人限购数量 */
    private Integer perUserLimit;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 状态：1启用 0停用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
