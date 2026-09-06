package com.artiv.guide.client;

import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@FunctionalInterface
interface GuideStreamTransport {
    void stream(HttpRequest request, int seconds, GuideCancellation cancellation, Consumer<String> event) throws Exception;

    static GuideStreamTransport jdk() {
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        return (request, seconds, cancellation, event) -> {
            cancellation.check();
            try (var timer = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon().factory())) {
                var deadline = timer.schedule(() -> cancellation.cancel("timeout"), seconds, TimeUnit.SECONDS);
                var pending = http.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream());
                cancellation.onCancel(() -> pending.cancel(true));
                try {
                    var response = pending.get(seconds, TimeUnit.SECONDS);
                    try (var body = response.body()) {
                        cancellation.onCancel(() -> { try { body.close(); } catch (Exception ignored) { } });
                        cancellation.check();
                        if (response.statusCode() != 200) throw new GuideModelException("模型服务返回 HTTP " + response.statusCode() + "，未自动重试");
                        if (!response.headers().firstValue("Content-Type").orElse("").toLowerCase().contains("text/event-stream")) {
                            throw new GuideModelException("模型服务没有返回事件流");
                        }
                        read(new InputStreamReader(body, StandardCharsets.UTF_8), cancellation, event);
                    }
                } finally { deadline.cancel(false); timer.shutdownNow(); }
            }
        };
    }

    /** 同时支持 CRLF、跨字节中文、注释心跳、多行 data；限制单帧与总响应。 */
    static void read(java.io.Reader reader, GuideCancellation cancellation, Consumer<String> event) throws Exception {
        var line = new StringBuilder();
        var data = new StringBuilder();
        char[] buffer = new char[2048];
        int total = 0, count;
        while ((count = reader.read(buffer)) != -1) {
            cancellation.check();
            if ((total += count) > 2_000_000) throw new GuideModelException("模型流超过容量限制");
            for (int i = 0; i < count; i++) {
                if (buffer[i] != '\n') {
                    line.append(buffer[i]);
                    if (line.length() > 65536) throw new GuideModelException("模型事件超过容量限制");
                    continue;
                }
                String value = line.toString().replaceFirst("\\r$", "");
                line.setLength(0);
                if (value.isEmpty()) {
                    if (!data.isEmpty()) { event.accept(data.substring(0, data.length() - 1)); data.setLength(0); }
                } else if (value.startsWith("data:")) {
                    String part = value.substring(5);
                    data.append(part.startsWith(" ") ? part.substring(1) : part).append('\n');
                    if (data.length() > 65536) throw new GuideModelException("模型事件超过容量限制");
                }
            }
        }
        // 不处理没有空行结束的半帧，由适配器判为中断，不能当成完整回答。
    }
}
