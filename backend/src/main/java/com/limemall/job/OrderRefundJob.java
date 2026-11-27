package com.limemall.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.limemall.entity.OmsOrderReturn;
import com.limemall.mapper.OmsOrderMapper;
import com.limemall.mapper.OmsOrderReturnMapper;
import com.limemall.service.AlipayService;
import com.limemall.service.OrderReturnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderRefundJob {

    private final OmsOrderReturnMapper returnMapper;
    private final OmsOrderMapper orderMapper;
    private final OrderReturnService returnService;
    private final AlipayService alipayService;

    @Scheduled(fixedDelay = 15_000, initialDelay = 15_000)
    public void processRefunds() {
        // 本轮只重试 30 秒前更新的申请。先更新时间，避免多个实例同时处理。
        LocalDateTime retryBefore = LocalDateTime.now().minusSeconds(30);
        var refunds = returnMapper.selectList(new LambdaQueryWrapper<OmsOrderReturn>()
                .eq(OmsOrderReturn::getStatus, OmsOrderReturn.PROCESSING)
                .le(OmsOrderReturn::getUpdateTime, retryBefore)
                .orderByAsc(OmsOrderReturn::getUpdateTime, OmsOrderReturn::getId).last("LIMIT 10"));
        for (OmsOrderReturn refund : refunds) {
            try {
                int claimed = returnMapper.update(null, new LambdaUpdateWrapper<OmsOrderReturn>()
                        .set(OmsOrderReturn::getUpdateTime, LocalDateTime.now())
                        .eq(OmsOrderReturn::getId, refund.getId())
                        .eq(OmsOrderReturn::getStatus, OmsOrderReturn.PROCESSING)
                        .le(OmsOrderReturn::getUpdateTime, retryBefore));
                if (claimed == 0) {
                    continue;
                }
                if (refund.getAmount().signum() == 0 || alipayService.refund(orderMapper.selectById(refund.getOrderId()), refund.getAmount())) {
                    returnService.confirmRefund(refund);
                } else {
                    log.warn("退款待支付渠道确认，稍后重试 orderSn={}", refund.getOrderSn());
                }
            } catch (Exception e) {
                log.warn("退款处理失败，稍后重试 orderSn={}：{}", refund.getOrderSn(), e.getMessage());
            }
        }
    }
}
