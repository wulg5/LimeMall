package com.limemall.util;

import java.util.regex.Pattern;

/**
 * 富文本消毒工具（商品详情等后台录入的 HTML）。
 *
 * 采用白名单思路的轻量实现：移除 script 标签、内联事件属性与 javascript: 协议，
 * 防止运营账号被撞库后向前台注入脚本。复杂场景（需求允许任意 HTML 标签）建议
 * 引入 OWASP Java HTML Sanitizer 做完整白名单过滤。
 */
public final class HtmlSanitizer {

    private static final Pattern SCRIPT = Pattern.compile("(?is)<script\\b.*?</script>");
    private static final Pattern EVENT_ATTR =
            Pattern.compile("(?i)\\son\\w+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)");
    private static final Pattern JS_PROTOCOL =
            Pattern.compile("(?i)(href|src)\\s*=\\s*(\"\\s*javascript:[^\"]*\"|'\\s*javascript:[^']*')");

    private HtmlSanitizer() {
    }

    public static String sanitize(String html) {
        if (html == null) {
            return null;
        }
        String cleaned = SCRIPT.matcher(html).replaceAll("");
        cleaned = EVENT_ATTR.matcher(cleaned).replaceAll("");
        cleaned = JS_PROTOCOL.matcher(cleaned).replaceAll("$1=\"#\"");
        return cleaned;
    }
}
