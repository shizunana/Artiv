package com.artiv.dto.guide;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 只接收本轮文本，不接受客户端系统提示、工具结果或伪造历史。 */
public record GuideMessageRequest(
        @NotBlank @Pattern(regexp = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}") String clientMessageId,
        @NotBlank @Size(max = 4000, message = "每条消息最多 4000 字") String text) { }
