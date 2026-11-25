package com.limemall.controller;

import com.limemall.service.AlipayService;
import com.limemall.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付回调接口（支付宝异步通知）。
 * notify 需公网可达：本地开发用 cloudflared 等内网穿透把本机 18080 映射为公网 HTTPS 地址。
 * 注意：支付宝要求纯文本响应 "success"/"failure"，不能返回 JSON。
 */
@Slf4j
@RestController
@RequestMapping("/portal/pay")
@RequiredArgsConstructor
public class PortalPayController {

    private final AlipayService alipayService;
    private final OrderService orderService;

    @PostMapping("/alipay/notify")
    public String alipayNotify(@RequestParam Map<String, String> params) {
        if (!alipayService.verifyNotify(params)) {
            log.warn("支付宝回调验签失败，out_trade_no={}", params.get("out_trade_no"));
            return "failure";
        }
        if (!alipayService.getAppId().equals(params.get("app_id"))) {
            log.warn("支付宝回调 app_id 不匹配：{}", params.get("app_id"));
            return "failure";
        }
        String tradeStatus = params.get("trade_status");
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            return "success";
        }
        String orderSn = params.get("out_trade_no");
        try {
            orderService.payByOrderSn(orderSn, new BigDecimal(params.get("total_amount")));
            log.info("支付宝回调入账成功：{}", orderSn);
            return "success";
        } catch (Exception e) {
            log.error("支付宝回调处理失败：{}", orderSn, e);
            return "failure";
        }
    }
}
