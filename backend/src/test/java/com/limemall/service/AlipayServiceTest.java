package com.limemall.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.limemall.common.BusinessException;
import com.limemall.config.AlipayProperties;
import com.limemall.dto.PayConfigSaveDTO;
import com.limemall.entity.OmsOrder;
import com.limemall.mapper.AlipayChannelProfileMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlipayServiceTest {
    private final ObjectMapper json = new ObjectMapper();
    private final AtomicBoolean confirmed = new AtomicBoolean();
    private final AtomicBoolean tampered = new AtomicBoolean();
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicReference<String> tradeStatus = new AtomicReference<>("TRADE_SUCCESS");
    private HttpServer gateway;
    private final AtomicReference<KeyPair> merchantKeys = new AtomicReference<>();
    private final AtomicReference<KeyPair> providerKeys = new AtomicReference<>();
    private final Map<String, String> profiles = new HashMap<>();
    private SysConfigService settings;
    private AlipayService service;

    @BeforeEach
    void startGateway() throws Exception {
        KeyPairGenerator keys = KeyPairGenerator.getInstance("RSA");
        keys.initialize(2048);
        KeyPair merchant = keys.generateKeyPair();
        merchantKeys.set(merchant);
        KeyPair provider = keys.generateKeyPair();
        providerKeys.set(provider);
        gateway = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        gateway.createContext("/", exchange -> {
            try (exchange) {
                try {
                    String form = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    Map<String, String> params = new TreeMap<>();
                    for (String part : form.split("&")) {
                        String[] pair = part.split("=", 2);
                        params.put(pair[0], URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
                    }
                    Signature verifier = Signature.getInstance("SHA256withRSA");
                    verifier.initVerify(merchantKeys.get().getPublic());
                    String signature = params.remove("sign");
                    String content = String.join("&", params.entrySet().stream()
                            .map(entry -> entry.getKey() + "=" + entry.getValue()).toList());
                    verifier.update(content.getBytes(StandardCharsets.UTF_8));
                    assertTrue(verifier.verify(Base64.getDecoder().decode(signature)));
                    var biz = json.readTree(params.get("biz_content"));
                    assertEquals("LM-fixture", biz.path("out_trade_no").asText());
                    assertEquals("test-only", params.get("app_id"));
                    String method = params.get("method");
                    var result = json.createObjectNode().put("code", "10000").put("out_trade_no", "LM-fixture");
                    switch (method) {
                        case "alipay.trade.refund" -> {
                            assertEquals("LMRF-LM-fixture", biz.path("out_request_no").asText());
                            assertEquals("94.52", biz.path("refund_amount").asText());
                            result.put("fund_change", "N");
                        }
                        case "alipay.trade.fastpay.refund.query" -> {
                            assertEquals("LMRF-LM-fixture", biz.path("out_request_no").asText());
                            result.put("refund_status", confirmed.get() ? "REFUND_SUCCESS" : "PROCESSING");
                            result.put("out_request_no", "LMRF-LM-fixture").put("refund_amount", "94.52");
                        }
                        case "alipay.trade.query" -> result.put("trade_status", tradeStatus.get())
                                .put("total_amount", "94.52").put("send_pay_date", "2026-11-27 10:00:00");
                        case "alipay.trade.close" -> assertEquals("WAIT_BUYER_PAY", tradeStatus.get());
                        default -> throw new AssertionError(method);
                    }
                    String raw = json.writerWithDefaultPrettyPrinter().writeValueAsString(result);
                    Signature signer = Signature.getInstance("SHA256withRSA");
                    signer.initSign(providerKeys.get().getPrivate());
                    signer.update(raw.getBytes(StandardCharsets.UTF_8));
                    String sign = Base64.getEncoder().encodeToString(signer.sign());
                    if (tampered.get()) {
                        raw = raw.replace("10000", "20000");
                    }
                    String response = "{\"" + method.replace('.', '_') + "_response\":" + raw
                            + ",\"sign\":\"" + sign + "\"}";
                    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                    requests.incrementAndGet();
                    exchange.sendResponseHeaders(200, bytes.length);
                    exchange.getResponseBody().write(bytes);
                } catch (Exception | AssertionError e) {
                    exchange.sendResponseHeaders(400, -1);
                }
            }
        });
        gateway.start();
        AlipayProperties config = new AlipayProperties();
        config.setAppId("test-only");
        config.setMerchantPrivateKey(Base64.getEncoder().encodeToString(merchant.getPrivate().getEncoded()));
        config.setAlipayPublicKey(Base64.getEncoder().encodeToString(provider.getPublic().getEncoded()));
        config.setGatewayUrl("http://127.0.0.1:" + gateway.getAddress().getPort());
        settings = mock(SysConfigService.class);
        when(settings.getAll()).thenReturn(Map.of(AlipayService.KEY_ENABLED, "0"));
        AlipayChannelProfileMapper profileMapper = mock(AlipayChannelProfileMapper.class);
        when(profileMapper.find(anyString())).thenAnswer(call -> profiles.get(call.getArgument(0)));
        doAnswer(call -> {
            profiles.putIfAbsent(call.getArgument(0), call.getArgument(1));
            return null;
        }).when(profileMapper).remember(anyString(), anyString());
        doAnswer(call -> {
            profiles.put(call.getArgument(0), call.getArgument(1));
            return null;
        }).when(profileMapper).saveCredentials(anyString(), anyString());
        service = new AlipayService(config, json, settings, profileMapper);
    }

    @AfterEach
    void stopGateway() {
        if (gateway != null) gateway.stop(0);
    }

    @Test
    void waitsForConfirmedRefundAndReusesRequestNumberOnRetry() {
        BigDecimal amount = new BigDecimal("94.52");
        assertFalse(service.refund(order(), amount));
        confirmed.set(true);
        assertTrue(service.refund(order(), amount));
        assertEquals(4, requests.get());
    }

    @Test
    void requiresNotifyUrlAndKeepsLocalReturnOnShopHost() {
        when(settings.getAll()).thenReturn(Map.of(AlipayService.KEY_ENABLED, "1"));
        assertFalse(service.isEnabled());
        assertThrows(BusinessException.class,
                () -> service.buildPagePayUrl(order(), LocalDateTime.now().plusMinutes(10), "127.0.0.1"));

        when(settings.getAll()).thenReturn(Map.of(AlipayService.KEY_ENABLED, "1",
                AlipayService.KEY_NOTIFY_URL, "https://notify.example.test/notify",
                AlipayService.KEY_RETURN_URL, "http://localhost:5173/shop/orders?from=x%20y"));
        assertTrue(service.isEnabled());
        var params = URI.create(service.buildPagePayUrl(
                order(), LocalDateTime.now().plusMinutes(10), "127.0.0.1").url()).getRawQuery();
        assertTrue(URLDecoder.decode(params, StandardCharsets.UTF_8)
                .contains("return_url=http://127.0.0.1:5173/shop/orders?from=x%20y"));
    }

    @Test
    void reconcilesSignedPaymentAndRejectsWrongAmount() {
        var result = service.checkPayment(order(), false);
        assertEquals(LocalDateTime.of(2026, 11, 27, 10, 0).atZone(ZoneId.of("Asia/Shanghai"))
                .withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(), result.paidAt());
        assertTrue(result.finished());

        OmsOrder otherAmount = order();
        otherAmount.setPayAmount(new BigDecimal("1.00"));
        assertThrows(BusinessException.class, () -> service.checkPayment(otherAmount, false));
    }

    @Test
    void closesUnpaidChannelTradeOnlyAfterDeadline() {
        tradeStatus.set("WAIT_BUYER_PAY");
        assertFalse(service.checkPayment(order(), false).finished());
        assertEquals(1, requests.get());
        assertTrue(service.checkPayment(order(), true).finished());
        assertEquals(3, requests.get());
    }

    @Test
    void olderOrdersUseTheirOriginalChannelAfterApplicationChange() throws Exception {
        useWritableSettings();
        LocalDateTime deadline = LocalDateTime.now().plusMinutes(10);
        OmsOrder pinned = order();
        var pay = service.buildPagePayUrl(pinned, deadline, null);
        pinned.setPaymentProfileId(pay.profileId());

        PayConfigSaveDTO replacement = new PayConfigSaveDTO();
        replacement.setEnabled(true);
        replacement.setAppId("new-application");
        replacement.setGatewayUrl("http://127.0.0.1:1");
        KeyPair replacementKeys = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        replacement.setMerchantPrivateKey(Base64.getEncoder().encodeToString(replacementKeys.getPrivate().getEncoded()));
        replacement.setAlipayPublicKey(Base64.getEncoder().encodeToString(replacementKeys.getPublic().getEncoded()));
        service.saveConfig(replacement);

        var retry = service.buildPagePayUrl(pinned, deadline, null);
        assertEquals(pay.profileId(), retry.profileId());
        assertTrue(retry.url().contains("app_id=test-only"));
        assertTrue(service.buildPagePayUrl(order(), deadline, null).url().contains("app_id=new-application"));

        Map<String, String> notification = signedNotify(providerKeys.get());
        assertTrue(service.verifyNotify(pinned, notification));
        assertTrue(service.verifyNotify(order(), notification));
        notification.put("app_id", "new-application");
        assertFalse(service.verifyNotify(pinned, notification));

        assertNotNull(service.checkPayment(pinned, false).paidAt());
        confirmed.set(true);
        assertTrue(service.refund(pinned, pinned.getPayAmount()));
        assertTrue(service.refund(order(), pinned.getPayAmount()));
        assertEquals(5, requests.get());
    }

    @Test
    void rotatesKeysForBoundOrdersEvenAfterSwitchingToAnotherApplication() throws Exception {
        useWritableSettings();
        OmsOrder pinned = order();
        LocalDateTime deadline = LocalDateTime.now().plusMinutes(10);
        pinned.setPaymentProfileId(service.buildPagePayUrl(pinned, deadline, null).profileId());
        Map<String, String> oldNotification = signedNotify(providerKeys.get());

        KeyPairGenerator keys = KeyPairGenerator.getInstance("RSA");
        keys.initialize(2048);
        KeyPair newMerchant = keys.generateKeyPair();
        KeyPair newProvider = keys.generateKeyPair();
        PayConfigSaveDTO rotated = new PayConfigSaveDTO();
        rotated.setEnabled(true);
        rotated.setMerchantPrivateKey(Base64.getEncoder().encodeToString(newMerchant.getPrivate().getEncoded()));
        rotated.setAlipayPublicKey(Base64.getEncoder().encodeToString(newProvider.getPublic().getEncoded()));
        service.saveConfig(rotated);
        merchantKeys.set(newMerchant);
        providerKeys.set(newProvider);

        assertEquals(pinned.getPaymentProfileId(), service.buildPagePayUrl(pinned, deadline, null).profileId());
        assertNotNull(service.checkPayment(pinned, false).paidAt());
        assertTrue(service.verifyNotify(pinned, signedNotify(newProvider)));
        assertFalse(service.verifyNotify(pinned, oldNotification));

        PayConfigSaveDTO replacement = new PayConfigSaveDTO();
        replacement.setEnabled(true);
        replacement.setAppId("another-application");
        replacement.setGatewayUrl("http://127.0.0.1:1");
        service.saveConfig(replacement);

        assertTrue(service.verifyNotify(pinned, signedNotify(newProvider)));
        confirmed.set(true);
        assertTrue(service.refund(pinned, pinned.getPayAmount()));
        assertEquals(3, requests.get());
    }

    private void useWritableSettings() {
        Map<String, String> active = new HashMap<>(Map.of(AlipayService.KEY_ENABLED, "1",
                AlipayService.KEY_NOTIFY_URL, "https://notify.example.test/notify"));
        when(settings.getAll()).thenAnswer(call -> new HashMap<>(active));
        doAnswer(call -> {
            active.put(call.getArgument(0), call.getArgument(1));
            return null;
        }).when(settings).put(anyString(), anyString(), any());
    }

    private Map<String, String> signedNotify(KeyPair key) throws Exception {
        Map<String, String> notification = new TreeMap<>(Map.of("app_id", "test-only", "out_trade_no", "LM-fixture"));
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(key.getPrivate());
        signer.update("app_id=test-only&out_trade_no=LM-fixture".getBytes(StandardCharsets.UTF_8));
        notification.put("sign", Base64.getEncoder().encodeToString(signer.sign()));
        return notification;
    }

    private OmsOrder order() {
        OmsOrder order = new OmsOrder();
        order.setOrderSn("LM-fixture");
        order.setPayAmount(new BigDecimal("94.52"));
        order.setStatus(OrderService.STATUS_UNPAID);
        return order;
    }

    @Test
    void rejectsTamperedProviderResponse() {
        tampered.set(true);
        assertThrows(BusinessException.class, () -> service.refund(order(), new BigDecimal("94.52")));
        assertEquals(1, requests.get());
    }
}
