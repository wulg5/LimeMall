package com.limemall.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 退款审核请求参数。
 */
@Data
public class ReturnHandleDTO {

    /** true=通过退款并关闭订单；false=拒绝 */
    @NotNull(message = "请选择审核结果")
    private Boolean approve;

    private String handleNote;
}
