package com.limemall.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动保存参数。
 */
@Data
public class FlashSaleSaveDTO {

    private Long id;

    @NotBlank(message = "请填写活动名称")
    @Size(max = 100, message = "活动名称不能超过 100 字")
    private String name;

    @NotNull(message = "请选择SKU")
    private Long skuId;

    @NotNull(message = "请填写秒杀价")
    @DecimalMin(value = "0.01", message = "秒杀价必须大于 0")
    private BigDecimal flashPrice;

    @NotNull(message = "请选择开始时间")
    private LocalDateTime startTime;

    @NotNull(message = "请选择结束时间")
    private LocalDateTime endTime;

    @NotNull(message = "请选择状态")
    private Integer status;
}
