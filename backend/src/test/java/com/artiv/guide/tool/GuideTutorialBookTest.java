package com.artiv.guide.tool;

import com.artiv.entity.User;
import com.artiv.mapper.UserMapper;
import com.artiv.guide.client.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class GuideTutorialBookTest {
    private final ObjectMapper json = new ObjectMapper();
    private final UserMapper users = mock(UserMapper.class);
    private User user(int role, int status, int deleted) {
        var user = new User(); user.setId(7L); user.setRole(role); user.setStatus(status); user.setDeleted(deleted);
        return user;
    }
    private GuideToolRegistry registry() throws Exception {
        return new GuideToolRegistry(json, new GuideRuleBook(json), mock(GuidePublicReader.class), null, new GuideTutorialBook(users));
    }
    private GuideModelClient.ToolCall call(String args) { return new GuideModelClient.ToolCall("t1", "read_artiv", args); }

    @Test void packagedCatalogContainsOnlyWhitelistedTopicsAndNoBodyOrInternalSources() throws Exception {
        var book = new GuideTutorialBook(users);
        var normal = json.valueToTree(book.read(null, null, List.of()));
        assertEquals(50, normal.path("topics").size());
        assertFalse(normal.toString().contains("ADMIN-")); assertTrue(normal.toString().contains("GUIDE-"));
        assertFalse(normal.toString().contains("body"));
        when(users.selectById(7L)).thenReturn(user(3, 0, 0));
        var admin = json.valueToTree(book.read(7L, null, List.of()));
        assertEquals(61, admin.path("topics").size());
        assertFalse(admin.toString().contains("维护索引")); assertFalse(admin.toString().contains("backend/"));
        assertFalse(admin.toString().contains("frontend/"));
        assertFalse(new org.springframework.core.io.ClassPathResource("guide/tutorials/维护索引.md").exists());
        assertFalse(new org.springframework.core.io.ClassPathResource("guide/tutorials/README.md").exists());
    }

    @Test void adminSearchAndDirectReadRecheckActualAccountEveryTime() throws Exception {
        var book = new GuideTutorialBook(users);
        when(users.selectById(7L)).thenReturn(user(3, 0, 0));
        assertTrue(json.writeValueAsString(book.read(7L, null, List.of("ADMIN-03"))).contains("调整后余额"));
        for (User forbidden : List.of(user(1,0,0), user(2,0,0), user(3,1,0), user(3,0,1))) {
            when(users.selectById(7L)).thenReturn(forbidden);
            assertThrows(GuideModelException.class, () -> book.read(7L, null, List.of("ACCOUNT-01", "ADMIN-03")));
            assertTrue(json.writeValueAsString(book.read(7L, null, List.of("GUIDE-01"))).contains("艺绘向导"));
            assertFalse(json.writeValueAsString(book.read(7L, "发放资金", List.of())).contains("ADMIN-"));
        }
        when(users.selectById(7L)).thenReturn(null);
        assertThrows(GuideModelException.class, () -> book.read(7L, null, List.of("ADMIN-03")));
    }

    @Test void searchFindsRealTasksAndPreservesTheirLimits() throws Exception {
        var book = new GuideTutorialBook(users);
        for (var entry : Map.of("发布企划", "PROJECT-01", "画师认证", "ACCOUNT-06",
                "删除邀请", "PROJECT-08", "修改库存", "SHOP-02", "验收", "ORDER-04").entrySet()) {
            var result = json.valueToTree(book.read(null, entry.getKey(), List.of()));
            assertTrue(result.path("topics").size() <= 3);
            assertTrue(result.toString().contains(entry.getValue()), entry.getKey());
        }
        String deletion = json.writeValueAsString(book.read(null, null, List.of("PROJECT-08")));
        assertTrue(deletion.contains("不改变邀请的业务状态"));
        assertTrue(json.valueToTree(book.read(null, "xyz123nomatch", List.of())).path("topics").isEmpty());
    }

    @Test void existingGuidesRemainDiscoverableWithoutFilePaths() throws Exception {
        var book = new GuideTutorialBook(users);
        var text = json.writeValueAsString(book.read(null, null, List.of("PROJECT-01", "ACCOUNT-06")));
        assertTrue(text.contains("/guides/employer")); assertTrue(text.contains("/guides/painter-certification"));
        assertFalse(text.contains(".vue")); assertFalse(text.contains("D:/"));
    }

    @Test void toolParametersCannotOverrideRoleReadFilesOrExceedBound() throws Exception {
        var registry = registry();
        for (String fields : List.of("\"role\":3", "\"ownerId\":7", "\"path\":\"../README.md\"",
                "\"topicIds\":[]", "\"topicIds\":[\"ADMIN-01\",\"ADMIN-02\",\"ADMIN-03\",\"ADMIN-04\"]",
                "\"topicIds\":[\"../维护索引.md\"]", "\"topicIds\":[12]", "\"keyword\":null",
                "\"keyword\":\"删除\",\"topicIds\":[\"PROJECT-04\"]")) {
            assertThrows(GuideModelException.class, () -> registry.execute(call("{\"operation\":\"tutorials\"," + fields + "}"), new GuideCancellation(), 7L));
        }
        assertThrows(GuideModelException.class, () -> registry.execute(call("{\"operation\":\"tutorials\",\"topicIds\":[\"UNKNOWN-01\"]}"), new GuideCancellation()));
    }

    @Test void tutorialBodiesNeverBecomeBrowserReferences() throws Exception {
        var registry = registry();
        var result = registry.execute(call("{\"operation\":\"tutorials\",\"keyword\":\"删除邀请\"}"), new GuideCancellation());
        assertTrue(result.content().contains("PROJECT-08")); assertTrue(result.references().isEmpty());
        var model = mock(GuideModelClient.class);
        when(model.complete(any())).thenReturn(new GuideModelClient.Reply("", List.of(call("{\"operation\":\"tutorials\",\"topicIds\":[\"PROJECT-08\"]}")), null, "tool_calls", 1),
                new GuideModelClient.Reply("", List.of(), null, "stop", 1));
        when(model.stream(any(), any(), any())).thenAnswer(invocation -> {
            var request = invocation.<GuideModelClient.Request>getArgument(0);
            assertTrue(request.messages().stream().anyMatch(m -> "tool".equals(m.role()) && m.text().contains("不改变邀请的业务状态")));
            return new GuideModelClient.Reply("在收到的邀请中打开删除邀请。", List.of(), null, "stop", 1);
        });
        var refs = new ArrayList<GuideReference>();
        new GuideAnswerRunner(registry).answer(model, List.of(GuideModelClient.Message.user("如何删除邀请")), s -> {},
                (name, value) -> { if (name.equals("references")) assertEquals(List.of(), value); }, refs::addAll, new GuideCancellation());
        assertTrue(refs.isEmpty());
    }

    @Test void howToFindArtistDoesNotForcePublicRecommendationOrAllowUngroundedAnswer() throws Exception {
        var model = mock(GuideModelClient.class);
        when(model.complete(any())).thenAnswer(invocation -> {
            var request = invocation.<GuideModelClient.Request>getArgument(0);
            assertTrue(request.requireTool());
            assertFalse(request.messages().stream().anyMatch(m -> m.text() != null && m.text().contains("本轮明确要找画师")));
            return new GuideModelClient.Reply("我猜你可以", List.of(), null, "stop", 1);
        });
        assertThrows(GuideModelException.class, () -> new GuideAnswerRunner(registry()).answer(model,
                List.of(GuideModelClient.Message.user("如何在平台找画师")), s -> {}, (n,v) -> {}, r -> {}, new GuideCancellation()));
        verify(model, never()).stream(any(), any(), any());
    }

    @Test void cancelledReadDoesNotLookupAccount() throws Exception {
        var registry = registry(); var cancellation = new GuideCancellation(); cancellation.cancel("stop");
        assertThrows(java.util.concurrent.CancellationException.class, () -> registry.execute(call("{\"operation\":\"tutorials\"}"), cancellation, 7L));
        verifyNoInteractions(users);
    }

    @Test void catalogAloneCannotBeUsedAsHowToEvidence() throws Exception {
        var model = mock(GuideModelClient.class);
        when(model.complete(any())).thenReturn(new GuideModelClient.Reply("", List.of(call("{\"operation\":\"tutorials\"}")), null, "tool_calls", 1),
                new GuideModelClient.Reply("目录足够了", List.of(), null, "stop", 1));
        assertThrows(GuideModelException.class, () -> new GuideAnswerRunner(registry()).answer(model,
                List.of(GuideModelClient.Message.user("怎么发布企划")), s -> {}, (n,v) -> {}, r -> {}, new GuideCancellation()));
        verify(model, never()).stream(any(), any(), any());
    }

    @Test void rulesAndTutorialsAgreeOnMultiSelectSubjects() throws Exception {
        var rules = registry().execute(call("{\"operation\":\"rules\"}"), new GuideCancellation()).content();
        var tutorial = json.writeValueAsString(new GuideTutorialBook(users).read(null, null, List.of("BROWSE-01")));
        assertTrue(rules.contains("题材可多选")); assertFalse(rules.contains("大厅题材单选"));
        assertTrue(tutorial.contains("不选类型或选多个类型时，题材隐藏并清空"));
    }
}
