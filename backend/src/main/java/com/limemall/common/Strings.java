package com.limemall.common;

import org.springframework.util.StringUtils;

/**
 * 字符串小工具。
 */
public final class Strings {

    private Strings() {
    }

    /** 空白字符串转 null（便于可选字段的清空与入库一致） */
    public static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
