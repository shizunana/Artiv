package com.artiv.guide.client;

import com.artiv.config.GuideConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/** 首个适配器。仅后端固定官方地址可收到 Key，模型/供应商不接受前端输入。 */
@Component
public class DeepSeekGuideModelClient implements GuideModelClient {
    private final GuideConfig config;
    private final ObjectMapper json;
    private final GuideHttpTransport transport;
    private final GuideStreamTransport streamTransport;

    @org.springframework.beans.factory.annotation.Autowired
    public DeepSeekGuideModelClient(GuideConfig config, ObjectMapper json) {
        this(config, json, GuideHttpTransport.jdk());
    }

    DeepSeekGuideModelClient(GuideConfig config, ObjectMapper json, GuideHttpTransport transport) {
        this(config, json, transport, GuideStreamTransport.jdk());
    }

    DeepSeekGuideModelClient(GuideConfig config, ObjectMapper json, GuideHttpTransport transport, GuideStreamTransport streamTransport) {
        this.config = config;
        this.json = json;
        this.transport = transport;
        this.streamTransport = streamTransport;
    }

    @Override
    public Reply complete(Request request) {
        validateConfig();
        long start = System.nanoTime();
        try {
            byte[] body = json.writeValueAsBytes(payload(request));
            if (body.length > 131072) throw new GuideModelException("模型请求超过 128 KiB 限制");
            var http = HttpRequest.newBuilder(URI.create("https://api.deepseek.com/chat/completions"))
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
            var response = transport.send(http, config.getTimeoutSeconds());
            if (response.status() != 200) throw new GuideModelException(safeStatus(response.status()));
            if (response.body().length > 262144) throw new GuideModelException("模型响应超过大小限制");
            var root = json.readTree(response.body());
            var choice = root.path("choices").path(0);
            var message = choice.path("message");
            if (!message.isObject()) throw new GuideModelException("模型响应缺少消息");
            var toolCalls = new ArrayList<ToolCall>();
            var rawTools = message.path("tool_calls");
            if (!rawTools.isMissingNode() && !rawTools.isNull()) {
                if (!rawTools.isArray() || rawTools.size() > 3) throw new GuideModelException("单次最多允许三个受控工具调用");
                for (var tool : rawTools) {
                    if (!"function".equals(tool.path("type").asText())) throw new GuideModelException("工具类型不受支持");
                    toolCalls.add(new ToolCall(requiredText(tool, "id"), requiredText(tool.path("function"), "name"),
                            requiredText(tool.path("function"), "arguments")));
                }
            }
            String content = message.path("content").asText("");
            String finish = choice.path("finish_reason").asText("");
            if (!Set.of("stop", "tool_calls").contains(finish) || content.isBlank() && toolCalls.isEmpty()) {
                throw new GuideModelException("模型输出为空、被截断或未正常结束");
            }
            if (toolCalls.stream().anyMatch(call -> request.tools().stream().noneMatch(t -> t.name().equals(call.name())))) {
                throw new GuideModelException("模型请求了未授权工具");
            }
            var usage = root.path("usage");
            return new Reply(content, java.util.List.copyOf(toolCalls),
                    new Usage(tokenCount(usage.path("prompt_tokens")), tokenCount(usage.path("completion_tokens"))),
                    finish, Duration.ofNanos(System.nanoTime() - start).toMillis());
        } catch (GuideModelException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GuideModelException("模型调用已取消");
        } catch (TimeoutException | HttpTimeoutException e) {
            throw new GuideModelException("模型调用超时，未自动重试");
        } catch (Exception e) {
            throw new GuideModelException("模型连接或响应解析失败，未自动重试");
        }
    }

    @Override
    public Reply stream(Request request, java.util.function.Consumer<String> onDelta, GuideCancellation cancellation) {
        validateConfig();
        long start = System.nanoTime();
        var content = new StringBuilder();
        String[] finish = {""};
        boolean[] done = {false};
        Usage[] usage = {new Usage(null, null)};
        try {
            if (!request.tools().isEmpty()) throw new GuideModelException("工具选择须使用受控非流式步骤，最终回答阶段不开放工具");
            var payload = payload(request);
            payload.put("stream", true);
            payload.putObject("stream_options").put("include_usage", true);
            byte[] body = json.writeValueAsBytes(payload);
            if (body.length > 131072) throw new GuideModelException("模型请求超过容量限制");
            var http = HttpRequest.newBuilder(URI.create("https://api.deepseek.com/chat/completions"))
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
            streamTransport.stream(http, config.getTimeoutSeconds(), cancellation, data -> {
                cancellation.check();
                if (done[0]) return;
                if ("[DONE]".equals(data)) { done[0] = true; return; }
                try {
                    var root = json.readTree(data);
                    if (root.hasNonNull("error")) throw new GuideModelException("模型流返回错误，未自动重试");
                    if (root.hasNonNull("usage")) usage[0] = new Usage(tokenCount(root.path("usage").path("prompt_tokens")), tokenCount(root.path("usage").path("completion_tokens")));
                    var choice = root.path("choices").path(0);
                    if (choice.path("delta").hasNonNull("tool_calls")) throw new GuideModelException("最终回答阶段未授权工具调用");
                    var delta = choice.path("delta").path("content");
                    if (delta.isTextual() && !delta.asText().isEmpty()) {
                        if (content.length() + delta.asText().length() > 32768) throw new GuideModelException("回答超过长度限制");
                        content.append(delta.asText());
                        onDelta.accept(delta.asText());
                    }
                    if (choice.hasNonNull("finish_reason")) finish[0] = choice.path("finish_reason").asText();
                } catch (GuideModelException | java.util.concurrent.CancellationException e) { throw e; }
                catch (Exception e) { throw new GuideModelException("模型流解析失败"); }
            });
            cancellation.check();
            if (!done[0] || content.isEmpty() || !Set.of("stop", "length").contains(finish[0])) {
                throw new GuideModelException("模型连接中断或回答未完整返回，未自动重试");
            }
            return new Reply(content.toString(), java.util.List.of(), usage[0], finish[0], Duration.ofNanos(System.nanoTime() - start).toMillis());
        } catch (GuideModelException e) { throw e; }
        catch (Exception e) {
            if (cancellation.cancelled()) throw new GuideModelException("timeout".equals(cancellation.reason()) ? "模型调用超时" : "生成已停止");
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new GuideModelException("模型流连接失败，未自动重试");
        }
    }

    private void validateConfig() {
        if (!config.isEnabled()) throw new GuideModelException("艺绘向导已关闭");
        if (!"deepseek".equals(config.getProvider())) throw new GuideModelException("尚未实现该供应商适配器");
        if (config.getApiKey() == null || config.getApiKey().isBlank()) throw new GuideModelException("未配置模型密钥");
        if (config.getTimeoutSeconds() < 1 || config.getTimeoutSeconds() > 300
                || config.getMaxOutputTokens() < 1 || config.getMaxOutputTokens() > 8192) {
            throw new GuideModelException("模型超时或输出配置超出安全范围");
        }
    }

    private ObjectNode payload(Request request) {
        if (request == null || request.messages() == null || request.messages().isEmpty() || request.messages().size() > 32
                || request.tools() == null || request.tools().size() > 1 || request.requireTool() && request.tools().isEmpty()) {
            throw new GuideModelException("模型请求参数无效");
        }
        String model = request.vision() ? config.getVisionModel() : config.getTextModel();
        if (model == null || !model.matches("deepseek-[a-z0-9-]{1,80}")) throw new GuideModelException("模型 ID 无效");
        var body = json.createObjectNode();
        body.put("model", model).put("stream", false).put("max_tokens", config.getMaxOutputTokens());
        body.putObject("thinking").put("type", "disabled");
        var messages = body.putArray("messages");
        int images = 0;
        for (var message : request.messages()) {
            if (message == null || !Set.of("system", "user", "assistant", "tool").contains(message.role())) {
                throw new GuideModelException("模型消息角色无效");
            }
            var item = messages.addObject().put("role", message.role());
            item.put("content", message.text() == null ? "" : message.text());
            if (message.imageDataUrl() != null) {
                if (++images > 1 || !request.vision() || !"user".equals(message.role())
                        || !message.imageDataUrl().matches("data:image/png;base64,[A-Za-z0-9+/]+={0,2}")) {
                    throw new GuideModelException("02 阶段只接受单张内联 PNG 测试图");
                }
                var parts = item.putArray("content");
                parts.addObject().put("type", "text").put("text", message.text());
                parts.addObject().put("type", "image_url").putObject("image_url").put("url", message.imageDataUrl());
            }
            if ("tool".equals(message.role())) item.put("tool_call_id", message.toolCallId());
            if (message.toolCalls() != null && !message.toolCalls().isEmpty()) {
                if (!"assistant".equals(message.role()) || message.toolCalls().size() > 3) throw new GuideModelException("工具消息无效");
                var tools = item.putArray("tool_calls");
                for (var call : message.toolCalls()) {
                    tools.addObject().put("id", call.id()).put("type", "function").putObject("function")
                            .put("name", call.name()).put("arguments", call.arguments());
                }
            }
        }
        if (!request.tools().isEmpty()) {
            var tools = body.putArray("tools");
            for (var tool : request.tools()) {
                var function = tools.addObject().put("type", "function").putObject("function");
                function.put("name", tool.name()).put("description", tool.description()).set("parameters", tool.parameters());
            }
            body.put("tool_choice", request.requireTool() ? "required" : "auto");
        }
        return body;
    }

    private static String requiredText(JsonNode node, String field) {
        var value = node.path(field);
        if (!value.isTextual() || value.asText().isBlank()) throw new GuideModelException("模型工具响应字段无效");
        return value.asText();
    }

    private static Long tokenCount(JsonNode value) {
        return value.isIntegralNumber() && value.canConvertToLong() && value.asLong() >= 0 ? value.asLong() : null;
    }

    private static String safeStatus(int status) {
        return switch (status) {
            case 401, 403 -> "模型鉴权失败或账号无权限";
            case 402 -> "模型账户余额不足";
            case 429 -> "模型服务限流，未自动重试";
            default -> "模型服务返回 HTTP " + status + "，未自动重试";
        };
    }
}
