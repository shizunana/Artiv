package com.artiv.guide.client;

import com.artiv.config.GuideConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class DeepSeekGuideModelClientTest {
    private final ObjectMapper json = new ObjectMapper();
    private static final String OK = "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"ok\"}}],\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":2}}";
    private final GuideModelClient.Request text = new GuideModelClient.Request(List.of(GuideModelClient.Message.user("hello")), false, List.of(), false);

    private GuideConfig config() {
        var config = new GuideConfig();
        config.setEnabled(true);
        config.setTimeoutSeconds(30);
        config.setMaxOutputTokens(512);
        config.setApiKey("test-placeholder-not-a-real-key");
        return config;
    }
    private GuideHttpTransport.Response response(int status, String body) {
        return new GuideHttpTransport.Response(status, body.getBytes(StandardCharsets.UTF_8));
    }
    private DeepSeekGuideModelClient client(GuideConfig config, GuideHttpTransport transport) {
        return new DeepSeekGuideModelClient(config, json, transport);
    }

    private com.fasterxml.jackson.databind.JsonNode readBody(java.net.http.HttpRequest request) throws Exception {
        var subscriber = java.net.http.HttpResponse.BodySubscribers.ofByteArray();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            public void onSubscribe(Flow.Subscription subscription) { subscriber.onSubscribe(subscription); }
            public void onNext(ByteBuffer buffer) { subscriber.onNext(List.of(buffer)); }
            public void onError(Throwable error) { subscriber.onError(error); }
            public void onComplete() { subscriber.onComplete(); }
        });
        return json.readTree(subscriber.getBody().toCompletableFuture().get(1, TimeUnit.SECONDS));
    }

    @Test void disabledMissingKeyOrUnsupportedProviderNeverSendsRequest() {
        var config = new GuideConfig();
        var client = client(config, (r, t) -> { fail("不应调用网络"); return null; });
        assertThrows(GuideModelException.class, () -> client.complete(text));
        config.setEnabled(true);
        assertThrows(GuideModelException.class, () -> client.complete(text));
        config.setApiKey("test-placeholder");
        config.setProvider("glm");
        assertThrows(GuideModelException.class, () -> client.complete(text));
    }
    @Test void acceptsBoundedMultipleToolCallsAndValidatesEveryName() throws Exception {
        var root = json.readTree("{\"choices\":[{\"finish_reason\":\"tool_calls\",\"message\":{\"content\":null,\"tool_calls\":[]}}]}");
        var calls = (com.fasterxml.jackson.databind.node.ArrayNode) root.path("choices").path(0).path("message").path("tool_calls");
        for (int i = 0; i < 3; i++) calls.addObject().put("id", "c" + i).put("type", "function").putObject("function").put("name", "read_artiv").put("arguments", "{}");
        var request = new GuideModelClient.Request(text.messages(), false, List.of(new GuideModelClient.ToolDefinition("read_artiv", "read", json.createObjectNode())), false);
        assertEquals(3, client(config(), (r,t) -> response(200, root.toString())).complete(request).toolCalls().size());
        ((com.fasterxml.jackson.databind.node.ObjectNode) calls.get(2).path("function")).put("name", "delete_all");
        assertThrows(GuideModelException.class, () -> client(config(), (r,t) -> response(200, root.toString())).complete(request));
        calls.add(calls.get(0).deepCopy());
        assertThrows(GuideModelException.class, () -> client(config(), (r,t) -> response(200, root.toString())).complete(request));
    }

    @Test void mapsTextImageToolsAndUsageWithBoundedOfficialRequest() throws Exception {
        var fixture = new GuideProbeFixture();
        var config = config();
        config.setTextModel("deepseek-v4-flash");
        AtomicInteger calls = new AtomicInteger();
        var client = client(config, (http, timeout) -> {
            assertEquals("https://api.deepseek.com/chat/completions", http.uri().toString());
            assertEquals(30, timeout);
            var body = readBody(http);
            assertEquals(512, body.path("max_tokens").asInt());
            assertEquals("disabled", body.path("thinking").path("type").asText());
            assertFalse(body.path("stream").asBoolean());
            if (calls.getAndIncrement() == 0) {
                assertEquals("deepseek-v4-flash", body.path("model").asText());
                assertEquals("hello", body.path("messages").path(0).path("content").asText());
            } else {
                assertEquals("deepseek-v4-flash-vision-exp", body.path("model").asText());
                assertEquals("image_url", body.path("messages").path(0).path("content").path(1).path("type").asText());
                assertEquals("required", body.path("tool_choice").asText());
                assertEquals(GuideProbeFixture.TOOL_NAME, body.path("tools").path(0).path("function").path("name").asText());
            }
            return response(200, OK);
        });
        var result = client.complete(text);
        assertEquals(10L, result.usage().inputTokens());
        assertEquals(2L, result.usage().outputTokens());
        client.complete(new GuideModelClient.Request(List.of(GuideModelClient.Message.image("identify", GuideProbeFixture.image())), true, List.of(fixture.tool()), true));
        assertEquals(2, calls.get());
    }

    @Test void failedCallsNeverRetryOrExposeBodyButDoNotBlockNewRequests() {
        var config = config();
        AtomicInteger calls = new AtomicInteger();
        var client = client(config, (r, t) -> { calls.incrementAndGet(); return response(401, "private key and body"); });
        var error = assertThrows(GuideModelException.class, () -> client.complete(text));
        assertEquals("模型鉴权失败或账号无权限", error.getMessage());
        assertNull(error.getCause());
        assertThrows(GuideModelException.class, () -> client.complete(text));
        assertEquals(2, calls.get());
    }

    @Test void timeoutIsSanitizedAndNotRetried() {
        var client = client(config(), (r, t) -> { throw new java.util.concurrent.TimeoutException("private data"); });
        assertEquals("模型调用超时，未自动重试", assertThrows(GuideModelException.class, () -> client.complete(text)).getMessage());
    }

    @Test void missingUsageIsUnknownNotZeroAndMalformedResponsesFail() {
        var client = client(config(), (r, t) -> response(200, "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"ok\"}}]}"));
        assertNull(client.complete(text).usage().inputTokens());
        for (String raw : List.of("bad json", "{}", OK.replace("stop", "length"), OK.replace("\"ok\"", "\"\""))) {
            assertThrows(GuideModelException.class, () -> client(config(), (r, t) -> response(200, raw)).complete(text));
        }
    }

    @Test void rejectsOversizedInputExternalImagesAndUnsafeConfigurationBeforeNetwork() {
        var config = config();
        var client = client(config, (r, t) -> { fail("不应调用网络"); return null; });
        assertThrows(GuideModelException.class, () -> client.complete(new GuideModelClient.Request(List.of(GuideModelClient.Message.user("a".repeat(131073))), false, List.of(), false)));
        assertThrows(GuideModelException.class, () -> client.complete(new GuideModelClient.Request(List.of(GuideModelClient.Message.image("x", "https://example.com/private.png")), true, List.of(), false)));
        config.setMaxOutputTokens(9000);
        assertThrows(GuideModelException.class, () -> client.complete(text));
    }

    @Test void validatesToolWhitelistArgumentsAndRoundTripMapping() throws Exception {
        var fixture = new GuideProbeFixture();
        var raw = "{\"choices\":[{\"finish_reason\":\"tool_calls\",\"message\":{\"tool_calls\":[{\"id\":\"call-1\",\"type\":\"function\",\"function\":{\"name\":\"read_probe_fixture\",\"arguments\":\"{}\"}}]}}]}";
        var reply = client(config(), (r, t) -> response(200, raw)).complete(new GuideModelClient.Request(text.messages(), false, List.of(fixture.tool()), true));
        var call = reply.toolCalls().getFirst();
        assertTrue(fixture.execute(call).contains(GuideProbeFixture.MARKER));
        assertThrows(GuideModelException.class, () -> fixture.execute(new GuideModelClient.ToolCall("1", "delete_order", "{}")));
        assertThrows(GuideModelException.class, () -> fixture.execute(new GuideModelClient.ToolCall("1", call.name(), "{\"userId\":1}")));
        assertThrows(Exception.class, () -> fixture.execute(new GuideModelClient.ToolCall("1", call.name(), "{} {}")));
        assertThrows(GuideModelException.class, () -> client(config(), (r, t) -> response(200, raw)).complete(text));
        client(config(), (r, t) -> {
            var body = readBody(r);
            assertEquals("call-1", body.path("messages").path(1).path("tool_calls").path(0).path("id").asText());
            assertEquals("call-1", body.path("messages").path(2).path("tool_call_id").asText());
            assertFalse(body.has("tools"));
            return response(200, OK);
        }).complete(new GuideModelClient.Request(List.of(text.messages().getFirst(), GuideModelClient.Message.assistant(reply), GuideModelClient.Message.tool(call.id(), fixture.execute(call))), false, List.of(), false));
    }

    @Test void responseSubscriberStopsAtHardByteLimit() {
        var body = new GuideHttpTransport.LimitedBody();
        AtomicInteger cancelled = new AtomicInteger();
        body.onSubscribe(new Flow.Subscription() {
            public void request(long n) { }
            public void cancel() { cancelled.incrementAndGet(); }
        });
        body.onNext(List.of(ByteBuffer.allocate(262145)));
        assertEquals(1, cancelled.get());
        assertTrue(body.getBody().toCompletableFuture().isCompletedExceptionally());
    }
}
