package com.artiv.guide.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/** 供应商无关的文本/图片/工具与流式契约；GLM 后续实现同一接口，不暴露上游原始响应给 UI。 */
public interface GuideModelClient {
    default String provider() { return "deepseek"; }
    Reply complete(Request request);
    default Reply stream(Request request, java.util.function.Consumer<String> onDelta, GuideCancellation cancellation) {
        throw new GuideModelException("该供应商尚未实现流式适配");
    }

    record Message(String role, String text, List<String> imageDataUrls, String toolCallId, List<ToolCall> toolCalls) {
        public Message { imageDataUrls = imageDataUrls == null ? List.of() : List.copyOf(imageDataUrls); }
        /** 兼容单图探针；新的视觉请求通过 imageDataUrls 保留全部图片顺序。 */
        public String imageDataUrl() { return imageDataUrls.isEmpty() ? null : imageDataUrls.getFirst(); }
        public static Message user(String text) { return new Message("user", text, null, null, List.of()); }
        public static Message image(String text, String dataUrl) { return images(text, List.of(dataUrl)); }
        public static Message images(String text, List<String> dataUrls) { return new Message("user", text, dataUrls, null, List.of()); }
        public static Message assistant(Reply reply) { return new Message("assistant", reply.text(), null, null, reply.toolCalls()); }
        public static Message tool(String id, String result) { return new Message("tool", result, null, id, List.of()); }
    }
    record ToolCall(String id, String name, String arguments) { }
    record ToolDefinition(String name, String description, JsonNode parameters) { }
    record Request(List<Message> messages, boolean vision, List<ToolDefinition> tools, boolean requireTool) { }
    record Usage(Long inputTokens, Long outputTokens) { }
    record Reply(String text, List<ToolCall> toolCalls, Usage usage, String finishReason, long elapsedMillis) { }
}
