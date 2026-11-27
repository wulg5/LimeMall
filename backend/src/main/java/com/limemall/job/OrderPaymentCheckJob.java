package com.limemall.job;

import com.limemall.entity.OmsOrder;
import com.limemall.service.AlipayService;
import com.limemall.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** 异步通知未送达时，从支付渠道补查付款结果。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaymentCheckJob {

    private final OrderService orderService;
    private final AlipayService alipayService;

    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    public void reconcilePayments() {
        LocalDateTime checkBefore = LocalDateTime.now().minusSeconds(30);
        for (OmsOrder order : orderService.listOrdersForPaymentCheck(checkBefore)) {
            try {
                if (!orderService.claimPaymentCheck(order, checkBefore)) {
                    continue;
                }
                AlipayService.PaymentCheck payment = alipayService.checkPayment(order, orderService.isPaymentExpired(order));
                if (payment.paidAt() != null) {
                    orderService.payByOrderSn(order.getOrderSn(), order.getPayAmount(), payment.paidAt());
                } else if (payment.finished()) {
                    orderService.finishUnpaidPaymentCheck(order.getId());
                }
            } catch (Exception e) {
                log.warn("支付宝查单等待重试 orderSn={}：{}", order.getOrderSn(), e.getMessage());
            }
        }
    }
}
