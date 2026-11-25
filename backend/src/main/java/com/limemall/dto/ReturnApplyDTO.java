package com.limemall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商城前台退款申请请求参数。
 */
@Data
public class ReturnApplyDTO {

    @NotNull(message = "订单不能为空")
    private Long orderId;

    @NotBlank(message = "请填写退款原因")
    private String reason;

    private String description;
}
