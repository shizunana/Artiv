package com.artiv.guide.client;

import com.artiv.config.GuideConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.io.StringReader;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GuideStreamingTest {
    private final GuideModelClient.Request request = new GuideModelClient.Request(List.of(GuideModelClient.Message.user("hi")), false, List.of(), false);
    private DeepSeekGuideModelClient client(GuideStreamTransport transport) {
        var config = new GuideConfig(); config.setEnabled(true); config.setApiKey("test-placeholder");
        return new DeepSeekGuideModelClient(config, new ObjectMapper(), (r, t) -> { throw new AssertionError("不得退回非流式调用"); }, transport);
    }
    @Test void streamsRealDeltasAndFinalUsageInsteadOfSimulatedTyping() {
        var output = new StringBuilder();
        var reply = client((http, seconds, cancel, event) -> {
            event.accept("{\"choices\":[{\"delta\":{\"content\":\"你\"},\"finish_reason\":null}]}");
            assertEquals("你", output.toString());
            event.accept("{\"choices\":[{\"delta\":{\"content\":\"好\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":12,\"completion_tokens\":2}}");
            event.accept("[DONE]");
        }).stream(request, output::append, new GuideCancellation());
        assertEquals("你好", reply.text());
        assertEquals(12L, reply.usage().inputTokens());
    }
    @Test void missingTerminatorAndUnexpectedToolsAreNotSuccessfulAnswers() {
        assertThrows(GuideModelException.class, () -> client((r, t, c, e) -> e.accept("{\"choices\":[{\"delta\":{\"content\":\"partial\"},\"finish_reason\":\"stop\"}]}"))
                .stream(request, ignored -> {}, new GuideCancellation()));
        assertThrows(GuideModelException.class, () -> client((r, t, c, e) -> e.accept("{\"choices\":[{\"delta\":{\"tool_calls\":[]}}]}"))
                .stream(request, ignored -> {}, new GuideCancellation()));
    }
    @Test void cancellationStopsCallbacks() {
        var cancellation = new GuideCancellation();
        assertThrows(GuideModelException.class, () -> client((r, t, c, e) -> { c.cancel("cancelled"); e.accept("[DONE]"); })
                .stream(request, ignored -> fail(), cancellation));
    }
    @Test void sseFramesSupportCrLfCommentsMultilineAndLimits() throws Exception {
        var events = new ArrayList<String>();
        GuideStreamTransport.read(new StringReader(": heartbeat\r\n\r\ndata: {\r\ndata: \"text\":\"中文\"}\r\n\r\ndata: [DONE]\n\n"), new GuideCancellation(), events::add);
        assertEquals(List.of("{\n\"text\":\"中文\"}", "[DONE]"), events);
        assertThrows(GuideModelException.class, () -> GuideStreamTransport.read(new StringReader("x".repeat(65537)), new GuideCancellation(), events::add));
    }
    @Test void routerReservesGlmButDoesNotMisrepresentItAsAvailable() {
        var config = new GuideConfig();
        var router = new GuideModelRouter(config, List.of(client((r,t,c,e) -> {})));
        assertTrue(router.supported());
        config.setProvider("glm");
        assertFalse(router.supported());
        assertThrows(GuideModelException.class, router::selected);
    }
}
