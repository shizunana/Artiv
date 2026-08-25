package com.artiv.common;

/**
 * 平台系统消息的对象标记规则。
 */
public final class SystemMessageText {
    private static final int TITLE_LIMIT = 10;

    private SystemMessageText() {
    }

    public static String mark(String value) {
        return "「" + normalize(value) + "」";
    }

    public static String markTitle(String value) {
        String normalized = normalize(value);
        int codePointCount = normalized.codePointCount(0, normalized.length());
        if (codePointCount > TITLE_LIMIT) {
            int endIndex = normalized.offsetByCodePoints(0, TITLE_LIMIT);
            normalized = normalized.substring(0, endIndex) + "...";
        }
        return mark(normalized);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
