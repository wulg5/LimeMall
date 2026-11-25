package com.limemall.job;

import com.limemall.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 支付超时关单定时任务。
 *
 * 每 60 秒扫描一次「待付款且超过支付时限」的订单并自动关闭，
 * 关闭时在同一事务内回补库存、回退销量、恢复优惠券，
 * 避免未支付订单长期占用库存（锁单/超卖防护的配套机制）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private final OrderService orderService;

    @Value("${limemall.order.pay-timeout-minutes:30}")
    private int payTimeoutMinutes;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void closeExpiredOrders() {
        List<Long> expiredIds = orderService.listExpiredOrderIds(payTimeoutMinutes);
        int closed = 0;
        for (Long orderId : expiredIds) {
            try {
                if (orderService.closeExpiredOrder(orderId, payTimeoutMinutes)) {
                    closed++;
                }
            } catch (Exception e) {
                log.warn("自动关单失败 orderId={}", orderId, e);
            }
        }
        if (closed > 0) {
            log.info("支付超时自动关闭订单 {} 笔（时限 {} 分钟）", closed, payTimeoutMinutes);
        }
    }
}
