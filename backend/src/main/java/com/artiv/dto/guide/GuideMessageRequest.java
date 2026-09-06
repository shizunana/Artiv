package com.artiv.dto.guide;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 本轮图文消息；只允许会话私有图片 ID，不接收图片 URL、系统提示或客户端历史。 */
public record GuideMessageRequest(
        @NotBlank @Pattern(regexp = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}") String clientMessageId,
        @Size(max = 4000, message = "每条消息最多 4000 字") String text,
        @Pattern(regexp = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}") String imageId,
        boolean imageConsent,
        @Size(max = 4, message = "每条消息最多 4 张参考图") java.util.List<@NotBlank @Pattern(regexp = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}") String> imageIds) {
    public GuideMessageRequest(String clientMessageId, String text, String imageId, boolean imageConsent) {
        this(clientMessageId, text, imageId, imageConsent, null);
    }
    public GuideMessageRequest(String clientMessageId, String text) { this(clientMessageId, text, null, false); }
    public java.util.List<String> attachmentIds() {
        return imageIds != null ? imageIds : imageId == null ? java.util.List.of() : java.util.List.of(imageId);
    }
    @jakarta.validation.constraints.AssertTrue(message = "请填写文字或添加最多 4 张参考图；图片不能重复，发送需同意分析，不可混用新旧图片字段")
    public boolean isValidContent() {
        var ids = attachmentIds();
        return !(imageId != null && imageIds != null) && ids.size() <= 4
                && ids.stream().allMatch(id -> id != null && !id.isBlank())
                && new java.util.HashSet<>(ids).size() == ids.size()
                && (text != null && !text.isBlank() || !ids.isEmpty()) && (ids.isEmpty() || imageConsent);
    }
}
