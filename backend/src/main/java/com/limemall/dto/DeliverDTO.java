package com.limemall.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 订单发货请求参数。
 */
@Data
public class DeliverDTO {

    @NotBlank(message = "请输入物流公司")
    private String logisticsCompany;

    @NotBlank(message = "请输入物流单号")
    private String logisticsSn;
}
