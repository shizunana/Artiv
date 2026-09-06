package com.artiv.guide.client;

/** 不携带上游正文、请求、密钥或原始异常链，避免全局日志泄露。 */
public class GuideModelException extends RuntimeException {
    public GuideModelException(String safeMessage) { super(safeMessage); }
}
