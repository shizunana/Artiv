package com.artiv.guide.tool;

import com.artiv.guide.client.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class GuideRulesTest {
    ObjectMapper json = new ObjectMapper();
    GuideToolRegistry registry() throws Exception { return new GuideToolRegistry(json, new GuideRuleBook(json), mock(GuidePublicReader.class)); }
    GuideModelClient.ToolCall call(String arguments) { return new GuideModelClient.ToolCall("call-1", "read_artiv", arguments); }
    @Test void rulesAreVersionedAndDoNotExposeInternalPaths() throws Exception {
        var result = registry().execute(call("{\"operation\":\"rules\"}"), new GuideCancellation());
        assertEquals(7, result.references().size());
        assertTrue(result.content().contains("已完成或已取消订单也属于关联订单"));
        assertTrue(result.content().contains("已完成、已取消订单不阻止删除橱窗"));
        assertTrue(result.content().contains("隐藏不等于拒绝或撤回邀请"));
        assertFalse(result.content().contains("backend/"));
        assertTrue(result.references().stream().allMatch(r -> r.kind().equals("rule") && !r.checkedAt().isBlank()));
    }
    @Test void arbitraryToolsUnknownFieldsDuplicateKeysAndTrailingJsonAreRejected() throws Exception {
        var registry = registry();
        for (String args : List.of("{}", "[]", "{\"operation\":\"delete\"}", "{\"operation\":\"rules\",\"actorId\":1}",
                "{\"operation\":\"rules\",\"operation\":\"rules\"}", "{\"operation\":\"rules\"}{}")) {
            assertThrows(GuideModelException.class, () -> registry.execute(call(args), new GuideCancellation()));
        }
        assertThrows(GuideModelException.class, () -> registry.execute(new GuideModelClient.ToolCall("x", "delete_project", "{}"), new GuideCancellation()));
    }
    @Test void cancelledRunCannotReadRules() throws Exception {
        var cancellation = new GuideCancellation(); cancellation.cancel("cancelled");
        var registry = registry();
        assertThrows(CancellationException.class, () -> registry.execute(call("{\"operation\":\"rules\"}"), cancellation));
    }
    @Test void toolRoundTripSuppliesEvidenceAndSumsUsageBeforeStreaming() throws Exception {
        var model = mock(GuideModelClient.class);
        var usage = new GuideModelClient.Usage(10L, 2L);
        when(model.complete(any())).thenReturn(new GuideModelClient.Reply("", List.of(call("{\"operation\":\"rules\"}")), usage, "tool_calls", 1),
                new GuideModelClient.Reply("准备回答", List.of(), usage, "stop", 1));
        when(model.stream(any(), any(), any())).thenAnswer(i -> {
            GuideModelClient.Request request = i.getArgument(0);
            assertTrue(request.tools().isEmpty());
            assertTrue(request.messages().stream().anyMatch(m -> m.role().equals("tool") && m.text().contains("企划删除条件")));
            i.<Consumer<String>>getArgument(1).accept("根据平台规则");
            return new GuideModelClient.Reply("根据平台规则", List.of(), usage, "stop", 1);
        });
        var refs = new ArrayList<GuideReference>(); var events = new ArrayList<String>(); var text = new StringBuilder();
        var reply = new GuideAnswerRunner(registry()).answer(model, List.of(GuideModelClient.Message.user("企划能删除吗")), text::append,
                (name, data) -> events.add(name), refs::addAll, new GuideCancellation());
        assertEquals("根据平台规则", text.toString());
        assertEquals(30L, reply.usage().inputTokens());
        assertEquals(7, refs.size()); assertTrue(events.contains("references"));
    }
    @Test void duplicateReadCannotLoopAndNoStreamAfterCancellation() throws Exception {
        var model = mock(GuideModelClient.class);
        when(model.complete(any())).thenReturn(new GuideModelClient.Reply("", List.of(call("{\"operation\":\"rules\"}")), null, "tool_calls", 1));
        var runner = new GuideAnswerRunner(registry());
        assertThrows(GuideModelException.class, () -> runner.answer(model, List.of(GuideModelClient.Message.user("x")), s -> {}, (n,d) -> {}, r -> {}, new GuideCancellation()));
        verify(model, times(2)).complete(any()); verify(model, never()).stream(any(), any(), any());
        var cancellation = new GuideCancellation();
        when(model.complete(any())).thenAnswer(i -> { cancellation.cancel("cancelled"); return new GuideModelClient.Reply("", List.of(), null, "stop", 1); });
        assertThrows(CancellationException.class, () -> runner.answer(model, List.of(GuideModelClient.Message.user("x")), s -> {}, (n,d) -> {}, r -> {}, cancellation));
        verify(model, never()).stream(any(), any(), any());
    }
    @Test void multipleCallsAreExecutedSequentiallyWithinOneTotalBudget() throws Exception {
        var tools = mock(GuideToolRegistry.class);
        when(tools.definition()).thenReturn(new GuideModelClient.ToolDefinition("read_artiv", "test", json.createObjectNode()));
        var order = new ArrayList<String>();
        when(tools.execute(any(), any())).thenAnswer(i -> { var call = i.<GuideModelClient.ToolCall>getArgument(0); order.add(call.id()); return new GuideToolRegistry.Result("{}", List.of()); });
        var model = mock(GuideModelClient.class);
        var calls = List.of(new GuideModelClient.ToolCall("a", "read_artiv", "a"), new GuideModelClient.ToolCall("b", "read_artiv", "b"), new GuideModelClient.ToolCall("c", "read_artiv", "c"));
        when(model.complete(any())).thenReturn(new GuideModelClient.Reply("", calls, null, "tool_calls", 1));
        when(model.stream(any(), any(), any())).thenAnswer(i -> {
            var request = i.<GuideModelClient.Request>getArgument(0);
            assertEquals(3, request.messages().stream().filter(m -> m.role().equals("tool")).count());
            return new GuideModelClient.Reply("answer", List.of(), null, "stop", 1);
        });
        new GuideAnswerRunner(tools).answer(model, List.of(GuideModelClient.Message.user("x")), s -> {}, (n,d) -> {}, r -> {}, new GuideCancellation());
        assertEquals(List.of("a", "b", "c"), order); verify(model, times(1)).complete(any());
        clearInvocations(tools, model);
        when(model.complete(any())).thenReturn(new GuideModelClient.Reply("", List.of(calls.get(0), calls.get(1)), null, "tool_calls", 1),
                new GuideModelClient.Reply("", List.of(calls.get(0), calls.get(2)), null, "tool_calls", 1));
        assertThrows(GuideModelException.class, () -> new GuideAnswerRunner(tools).answer(model, List.of(GuideModelClient.Message.user("x")), s -> {}, (n,d) -> {}, r -> {}, new GuideCancellation()));
        verify(tools, times(2)).execute(any(), any()); verify(model, never()).stream(any(), any(), any());
    }
}
