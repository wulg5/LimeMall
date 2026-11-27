package com.limemall.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商城前台下单请求参数。
 */
@Data
public class OrderCreateDTO {

    /** 下单明细（购物车勾选或立即购买） */
    @NotEmpty(message = "请选择要购买的商品")
    private List<@NotNull @Valid Item> items;

    @NotBlank(message = "请填写收货人姓名")
    private String receiverName;

    @NotBlank(message = "请填写收货人电话")
    private String receiverPhone;

    @NotBlank(message = "请填写收货地址")
    private String receiverAddress;

    private String note;

    /** 使用的会员优惠券ID（sms_coupon_member.id），可为空 */
    private Long couponMemberId;

    @DecimalMin(value = "0", message = "结算金额不能为负数")
    private BigDecimal expectedPayAmount;

    /**
     * 下单明细子项。
     */
    @Data
    public static class Item {

        @NotNull(message = "SKU 不能为空")
        private Long skuId;

        @NotNull(message = "购买数量不能为空")
        @Min(value = 1, message = "购买数量至少为 1")
        private Integer quantity;

        @DecimalMin(value = "0.01", message = "确认单价必须大于 0")
        private BigDecimal expectedUnitPrice;
    }
}
