package com.limemall.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 订单评价提交参数（一张订单可对多个商品逐条评价）。
 * 资格校验：订单归属当前会员且状态为「已完成」；同一订单同一商品仅可评价一次。
 */
@Data
public class CommentSubmitDTO {

    @NotNull(message = "订单不能为空")
    private Long orderId;

    @NotEmpty(message = "请填写评价")
    @Valid
    private List<Item> comments;

    @Data
    public static class Item {

        @NotNull(message = "商品不能为空")
        private Long productId;

        @NotNull(message = "请选择评分")
        @Min(value = 1, message = "评分为 1-5 星")
        @Max(value = 5, message = "评分为 1-5 星")
        private Integer rating;

        @NotBlank(message = "请填写评价内容")
        @Size(max = 500, message = "评价内容不能超过 500 字")
        private String content;
    }
}
