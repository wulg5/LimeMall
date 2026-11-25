package com.limemall.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图形验证码服务（内存实现，防登录爆破）。
 *
 * - 4 位字符（去除易混淆字符），2 分钟有效，**一次性使用**（校验即销毁，含失败场景）；
 * - 登录失败 2 次后强制要求验证码（见 AuthService / PortalAuthService）；
 * - 单机内存实现满足课设/单实例部署；多实例部署应替换为 Redis。
 */
@Slf4j
@Service
public class CaptchaService {

    private static final long TTL_MILLIS = 2 * 60 * 1000L;
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int MAX_STORE = 10_000;

    private record Entry(String code, long expireAt) {
    }

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    /** 生成验证码：返回 captchaId 与 dataURL 图片 */
    public Map<String, String> generate() {
        purgeExpired();
        if (store.size() > MAX_STORE) {
            store.clear();
        }
        StringBuilder code = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            code.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        store.put(captchaId, new Entry(code.toString(), System.currentTimeMillis() + TTL_MILLIS));
        return Map.of(
                "captchaId", captchaId,
                "image", "data:image/png;base64," + render(code.toString()));
    }

    /** 校验验证码（一次性：无论成败都销毁，防重放） */
    public boolean verify(String captchaId, String input) {
        if (captchaId == null || input == null) {
            return false;
        }
        Entry entry = store.remove(captchaId);
        return entry != null
                && entry.expireAt() >= System.currentTimeMillis()
                && entry.code().equalsIgnoreCase(input.trim());
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(item -> item.getValue().expireAt() < now);
    }

    /** 渲染验证码 PNG（Base64） */
    private String render(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setPaint(new GradientPaint(0, 0, new Color(240, 250, 245),
                    WIDTH, HEIGHT, new Color(210, 238, 226)));
            graphics.fillRect(0, 0, WIDTH, HEIGHT);

            // 干扰线
            graphics.setStroke(new BasicStroke(1.2f));
            for (int i = 0; i < 6; i++) {
                graphics.setColor(new Color(120 + random.nextInt(120),
                        140 + random.nextInt(100), 130 + random.nextInt(110)));
                graphics.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                        random.nextInt(WIDTH), random.nextInt(HEIGHT));
            }

            // 字符（随机旋转与偏移，增加机器识别难度）
            graphics.setFont(new Font("Arial", Font.BOLD, 26));
            for (int i = 0; i < code.length(); i++) {
                AffineTransform original = graphics.getTransform();
                double angle = (random.nextDouble() - 0.5) * 0.5;
                graphics.rotate(angle, 18 + i * 26, 26);
                graphics.setColor(new Color(30 + random.nextInt(90),
                        70 + random.nextInt(90), 60 + random.nextInt(80)));
                graphics.drawString(String.valueOf(code.charAt(i)),
                        12 + i * 26, 29 + random.nextInt(5));
                graphics.setTransform(original);
            }
        } finally {
            graphics.dispose();
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            log.error("验证码渲染失败", e);
            return "";
        }
    }
}
