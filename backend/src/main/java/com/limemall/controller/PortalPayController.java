package com.limemall.controller;

import com.limemall.common.BusinessException;
import com.limemall.common.Result;
import com.limemall.service.AlipayService;
import com.limemall.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付回调接口。
 * - /alipay/notify：支付宝异步通知（需公网可达），响应纯文本 success/failure；
 * - /alipay/sync-notify：同步回跳验签入账（无需公网：浏览器付款后 302 回本机，参数由支付宝私钥签名）。
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

    /**
     * 同步回跳验签入账（无公网方案的核心通道）。
     * 支付宝收银台付款成功后把浏览器回跳到 return_url，参数携带支付宝私钥签名——验签通过即证明支付真实发生。
     */
    @PostMapping("/alipay/sync-notify")
    public Result<Void> alipaySyncNotify(@RequestBody Map<String, String> params) {
        if (!alipayService.verifyNotify(params)) {
            throw new BusinessException("支付结果验签失败");
        }
        if (!alipayService.getAppId().equals(params.get("app_id"))) {
            throw new BusinessException("支付结果来源校验失败");
        }
        String orderSn = params.get("out_trade_no");
        if (!StringUtils.hasText(orderSn)) {
            throw new BusinessException("支付结果缺少订单号");
        }
        orderService.payByOrderSn(orderSn, new BigDecimal(params.get("total_amount")));
        log.info("支付宝同步回跳入账成功：{}", orderSn);
        return Result.ok();
    }
}
