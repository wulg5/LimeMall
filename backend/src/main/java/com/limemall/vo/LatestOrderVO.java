package com.limemall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 看板最新订单条目。
 */
@Data
public class LatestOrderVO {

    private Long id;

    private String orderSn;

    /** 会员昵称 */
    private String nickname;

    private BigDecimal payAmount;

    private Integer status;

    private LocalDateTime createTime;
}
