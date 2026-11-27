package com.limemall.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.limemall.common.BusinessException;
import com.limemall.config.AlipayProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlipayServiceTest {
    private final ObjectMapper json = new ObjectMapper();
    private final AtomicBoolean confirmed = new AtomicBoolean();
    private final AtomicBoolean tampered = new AtomicBoolean();
    private final AtomicInteger requests = new AtomicInteger();
    private HttpServer gateway;
    private AlipayService service;

    @BeforeEach
    void startGateway() throws Exception {
        KeyPairGenerator keys = KeyPairGenerator.getInstance("RSA");
        keys.initialize(2048);
        KeyPair merchant = keys.generateKeyPair();
        KeyPair provider = keys.generateKeyPair();
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
                    verifier.initVerify(merchant.getPublic());
                    String signature = params.remove("sign");
                    String content = String.join("&", params.entrySet().stream()
                            .map(entry -> entry.getKey() + "=" + entry.getValue()).toList());
                    verifier.update(content.getBytes(StandardCharsets.UTF_8));
                    assertTrue(verifier.verify(Base64.getDecoder().decode(signature)));
                    var biz = json.readTree(params.get("biz_content"));
                    assertEquals("LM-fixture", biz.path("out_trade_no").asText());
                    assertEquals("LMRF-LM-fixture", biz.path("out_request_no").asText());
                    String method = params.get("method");
                    var result = json.createObjectNode().put("code", "10000").put("out_trade_no", "LM-fixture");
                    if ("alipay.trade.refund".equals(method)) {
                        assertEquals("94.52", biz.path("refund_amount").asText());
                        result.put("fund_change", "N");
                    } else {
                        assertEquals("alipay.trade.fastpay.refund.query", method);
                        result.put("refund_status", confirmed.get() ? "REFUND_SUCCESS" : "PROCESSING");
                        result.put("out_request_no", "LMRF-LM-fixture").put("refund_amount", "94.52");
                    }
                    String raw = json.writerWithDefaultPrettyPrinter().writeValueAsString(result);
                    Signature signer = Signature.getInstance("SHA256withRSA");
                    signer.initSign(provider.getPrivate());
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
        SysConfigService settings = mock(SysConfigService.class);
        when(settings.getAll()).thenReturn(Map.of(AlipayService.KEY_ENABLED, "0"));
        service = new AlipayService(config, json, settings);
    }

    @AfterEach
    void stopGateway() {
        if (gateway != null) gateway.stop(0);
    }

    @Test
    void waitsForConfirmedRefundAndReusesRequestNumberOnRetry() {
        BigDecimal amount = new BigDecimal("94.52");
        assertFalse(service.refund("LM-fixture", amount));
        confirmed.set(true);
        assertTrue(service.refund("LM-fixture", amount));
        assertEquals(4, requests.get());
    }

    @Test
    void rejectsTamperedProviderResponse() {
        tampered.set(true);
        assertThrows(BusinessException.class, () -> service.refund("LM-fixture", new BigDecimal("94.52")));
        assertEquals(1, requests.get());
    }
}
