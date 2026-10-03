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
        assertEquals(8, result.references().size());
        assertTrue(result.content().contains("已完成或已取消订单也属于关联订单"));
        assertTrue(result.content().contains("已完成、已取消订单不阻止删除橱窗"));
        assertTrue(result.content().contains("隐藏不等于拒绝或撤回邀请"));
        assertFalse(result.content().contains("backend/"));
        assertTrue(result.references().stream().allMatch(r -> r.kind().equals("rule") && !r.checkedAt().isBlank()));
        assertTrue(result.references().stream().allMatch(r -> r.card() == null));
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
    @Test void projectStallAndArtistSearchUseSeparateCategoryContracts() throws Exception {
        var reader = mock(GuidePublicReader.class);
        when(reader.projects(1,null,"角色设计",null,null,null)).thenReturn(new GuidePublicReader.ReadResult(List.of(),List.of()));
        when(reader.stalls(1,null,"头像",null,null,null)).thenReturn(new GuidePublicReader.ReadResult(List.of(),List.of()));
        when(reader.artists(1,null,"Live2d",null)).thenReturn(new GuidePublicReader.ReadResult(List.of(),List.of()));
        var registry = new GuideToolRegistry(json,new GuideRuleBook(json),reader);
        registry.execute(call("{\"operation\":\"search_projects\",\"projectCategory\":\"角色设计\"}"),new GuideCancellation());
        registry.execute(call("{\"operation\":\"search_stalls\",\"category\":\"头像\"}"),new GuideCancellation());
        registry.execute(call("{\"operation\":\"search_artists\",\"category\":\"Live2d\"}"),new GuideCancellation());
        verify(reader).projects(1,null,"角色设计",null,null,null);
        verify(reader).stalls(1,null,"头像",null,null,null);
        verify(reader).artists(1,null,"Live2d",null);
        for (String invalid : List.of(
                "{\"operation\":\"search_projects\",\"category\":\"头像\"}",
                "{\"operation\":\"search_projects\",\"projectCategory\":\"Live2d\"}",
                "{\"operation\":\"search_artists\",\"projectCategory\":\"头像\"}",
                "{\"operation\":\"search_stalls\",\"projectCategory\":\"头像\"}",
                "{\"operation\":\"search_projects\",\"projectCategory\":\"头像\",\"category\":\"头像\"}")) {
            assertThrows(GuideModelException.class,()->registry.execute(call(invalid),new GuideCancellation()));
        }
        verifyNoMoreInteractions(reader);
    }
    @Test void stallSearchUsesPriceFieldsAndOriginalSubjects() throws Exception {
        var reader = mock(GuidePublicReader.class);
        var min = new java.math.BigDecimal("100"); var max = new java.math.BigDecimal("300");
        when(reader.stalls(1,"头像","头像","Q版",min,max)).thenReturn(new GuidePublicReader.ReadResult(List.of(),List.of()));
        var registry = new GuideToolRegistry(json,new GuideRuleBook(json),reader);
        registry.execute(call("{\"operation\":\"search_stalls\",\"keyword\":\"头像\",\"category\":\"头像\",\"subject\":\"Q版\",\"priceMin\":100,\"priceMax\":300}"),new GuideCancellation());
        verify(reader).stalls(1,"头像","头像","Q版",min,max);
        for (String invalid : List.of(
                "{\"operation\":\"search_stalls\",\"budgetMin\":100}",
                "{\"operation\":\"search_stalls\",\"priceMin\":300,\"priceMax\":100}"))
            assertThrows(GuideModelException.class, () -> registry.execute(call(invalid), new GuideCancellation()));
        verifyNoMoreInteractions(reader);
    }
    @Test void projectSubjectsAreRestrictedButArtistSubjectsStayOriginal() throws Exception {
        var reader = mock(GuidePublicReader.class);
        when(reader.projects(1,null,"头像","安头",null,null)).thenReturn(new GuidePublicReader.ReadResult(List.of(),List.of()));
        when(reader.artists(1,null,null,"动图")).thenReturn(new GuidePublicReader.ReadResult(List.of(),List.of()));
        var registry = new GuideToolRegistry(json,new GuideRuleBook(json),reader);
        registry.execute(call("{\"operation\":\"search_projects\",\"projectCategory\":\"头像\",\"subject\":\"安头\"}"),new GuideCancellation());
        registry.execute(call("{\"operation\":\"search_artists\",\"subject\":\"动图\"}"),new GuideCancellation());
        for (String value: List.of("动图","场景")) {
            assertThrows(GuideModelException.class, () -> registry.execute(call("{\"operation\":\"search_projects\",\"projectCategory\":\"头像\",\"subject\":\""+value+"\"}"),new GuideCancellation()));
        }
        verify(reader).projects(1,null,"头像","安头",null,null);
        verify(reader).artists(1,null,null,"动图"); verifyNoMoreInteractions(reader);
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
            assertFalse(request.messages().stream().anyMatch(m -> m.text() != null && m.text().contains("前端会在本条回复下自动展示")));
            i.<Consumer<String>>getArgument(1).accept("根据平台规则");
            return new GuideModelClient.Reply("根据平台规则", List.of(), usage, "stop", 1);
        });
        var refs = new ArrayList<GuideReference>(); var events = new ArrayList<String>(); var text = new StringBuilder();
        var reply = new GuideAnswerRunner(registry()).answer(model, List.of(GuideModelClient.Message.user("企划能删除吗")), text::append,
                (name, data) -> events.add(name), refs::addAll, new GuideCancellation());
        assertEquals("根据平台规则", text.toString());
        assertEquals(30L, reply.usage().inputTokens());
        assertEquals(8, refs.size()); assertTrue(events.contains("references"));
    }
    @Test void resourceCardsAreEmittedAndModelIsAskedNotToRepeatCandidateLists() throws Exception {
        var reader = mock(GuidePublicReader.class);
        var ref = new GuideReference("project", "11", "测试企划", "内部查询摘要", "now",
                new GuideReference.Card("头像", List.of(new GuideReference.Field("预算", "100 元")), "需求"));
        when(reader.projects(1, null, null, null, null, null))
                .thenReturn(new GuidePublicReader.ReadResult(List.of(Map.of("id", "11", "title", "测试企划")), List.of(ref)));
        var tools = new GuideToolRegistry(json, new GuideRuleBook(json), reader);
        var model = mock(GuideModelClient.class);
        when(model.complete(any())).thenReturn(new GuideModelClient.Reply("", List.of(call("{\"operation\":\"search_projects\"}")), null, "tool_calls", 1),
                new GuideModelClient.Reply("", List.of(), null, "stop", 1));
        when(model.stream(any(), any(), any())).thenAnswer(i -> {
            var request = i.<GuideModelClient.Request>getArgument(0);
            var instruction = request.messages().stream().filter(m -> "system".equals(m.role()) && m.text().contains("前端会在本条回复下自动展示")).findFirst().orElseThrow().text();
            assertTrue(instruction.contains("project:11") && instruction.contains("不要再逐条列举候选"));
            assertTrue(instruction.contains("仍应回答问题所需的分析"));
            return new GuideModelClient.Reply("可以点击下方卡片查看。", List.of(), null, "stop", 1);
        });
        var saved = new ArrayList<GuideReference>(); var emitted = new ArrayList<Object>();
        new GuideAnswerRunner(tools).answer(model, List.of(GuideModelClient.Message.user("找企划")), s -> {},
                (name, value) -> { if (name.equals("references")) emitted.add(value); }, saved::addAll, new GuideCancellation());
        assertEquals(List.of(ref), saved);
        assertEquals(List.of(List.of(ref)), emitted);
    }
    @Test void artistBatchHasBoundedValidatedCategoriesAndNoArbitraryFilters() throws Exception {
        var reader = mock(GuidePublicReader.class);
        when(reader.artistsByCategory(anyList(), isNull(), any())).thenReturn(new GuidePublicReader.ReadResult(List.of(), List.of()));
        var registry = new GuideToolRegistry(json, new GuideRuleBook(json), reader);
        registry.execute(call("{\"operation\":\"recommend_artists_by_category\",\"categories\":[\"头像\",\"立绘\",\"头像\"]}"), new GuideCancellation());
        verify(reader).artistsByCategory(eq(List.of("头像", "立绘")), isNull(), any());
        for (String args : List.of("{\"categories\":[]}", "{\"categories\":[\"角色设计\"]}", "{\"ownerId\":3}", "{\"categories\":\"头像\"}")) {
            assertThrows(GuideModelException.class, () -> registry.execute(call(args.replace("{", "{\"operation\":\"recommend_artists_by_category\",")), new GuideCancellation()));
        }
    }
    @Test void explicitArtistSearchRequiresRealToolExecutionBeforeAnswering() throws Exception {
        var model = mock(GuideModelClient.class);
        when(model.complete(any())).thenAnswer(i -> {
            assertTrue(i.<GuideModelClient.Request>getArgument(0).requireTool());
            return new GuideModelClient.Reply("请稍等", List.of(), null, "stop", 1);
        });
        assertThrows(GuideModelException.class, () -> new GuideAnswerRunner(registry()).answer(model,
                List.of(GuideModelClient.Message.user("能找点画师给我参考下吗")), s -> {}, (n,d) -> {}, r -> {}, new GuideCancellation()));
        verify(model, never()).stream(any(), any(), any());
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
