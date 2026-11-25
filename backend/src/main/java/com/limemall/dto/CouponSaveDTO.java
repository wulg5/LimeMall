package com.limemall.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券新增/编辑请求参数。
 */
@Data
public class CouponSaveDTO {

    /** 编辑时必填 */
    private Long id;

    @NotBlank(message = "请输入优惠券名称")
    private String name;

    /** 1满减券 2折扣券 */
    @NotNull(message = "请选择优惠券类型")
    @Min(value = 1, message = "优惠券类型不正确")
    @Max(value = 2, message = "优惠券类型不正确")
    private Integer type;

    @NotNull(message = "请输入满减金额或折扣")
    @DecimalMin(value = "0.01", message = "金额或折扣必须大于 0")
    private BigDecimal amount;

    @NotNull(message = "请输入使用门槛")
    @DecimalMin(value = "0", message = "使用门槛不能为负数")
    private BigDecimal minAmount;

    @NotNull(message = "请输入发行总量")
    @Min(value = 1, message = "发行总量至少为 1")
    private Integer totalCount;

    @NotNull(message = "请输入每人限领数量")
    @Min(value = 1, message = "每人限领至少为 1")
    private Integer perLimit;

    @NotNull(message = "请选择有效期开始时间")
    private LocalDateTime startTime;

    @NotNull(message = "请选择有效期结束时间")
    private LocalDateTime endTime;
}
