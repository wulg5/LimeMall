package com.limemall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款申请列表项视图。
 */
@Data
public class ReturnVO {

    private Long id;

    private Long orderId;

    private String orderSn;

    private Long memberId;

    private String memberNickname;

    private BigDecimal amount;

    private String reason;

    private String description;

    /** 0待审核 1已通过 2已拒绝 */
    private Integer status;

    private String handleNote;

    private LocalDateTime handleTime;

    private LocalDateTime createTime;
}
