package com.artiv.dto.guide;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 本轮图文消息；只允许会话私有图片 ID，不接收图片 URL、系统提示或客户端历史。 */
public record GuideMessageRequest(
        @NotBlank @Pattern(regexp = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}") String clientMessageId,
        @Size(max = 4000, message = "每条消息最多 4000 字") String text,
        @Pattern(regexp = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}") String imageId,
        boolean imageConsent) {
    public GuideMessageRequest(String clientMessageId, String text) { this(clientMessageId, text, null, false); }
    @jakarta.validation.constraints.AssertTrue(message = "请填写文字或添加一张参考图；发送图片需同意分析")
    public boolean isValidContent() { return (text != null && !text.isBlank() || imageId != null) && (imageId == null || imageConsent); }
}
