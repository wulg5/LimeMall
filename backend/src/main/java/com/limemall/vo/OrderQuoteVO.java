package com.limemall.vo;

import com.limemall.entity.OmsOrderItem;

import java.math.BigDecimal;
import java.util.List;

public record OrderQuoteVO(List<OmsOrderItem> items, BigDecimal totalAmount, BigDecimal freightAmount,
                           BigDecimal couponAmount, BigDecimal payAmount) {}
