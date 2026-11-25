package com.limemall.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 商城前台退款申请请求参数。
 * 订单ID由接口路径回填（不从前端请求体接收，避免两处来源不一致）。
 */
@Data
public class ReturnApplyDTO {

    /** 订单ID（由接口路径回填） */
    private Long orderId;

    @NotBlank(message = "请填写退款原因")
    private String reason;

    private String description;
}
