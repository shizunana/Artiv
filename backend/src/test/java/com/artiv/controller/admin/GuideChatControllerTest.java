package com.artiv.controller.admin;

import com.artiv.common.GlobalExceptionHandler;
import com.artiv.config.GuideConfig;
import com.artiv.entity.User;
import com.artiv.guide.client.*;
import com.artiv.guide.session.GuideSessionStore;
import com.artiv.mapper.UserMapper;
import com.artiv.security.*;
import com.artiv.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GuideChatControllerTest {
    GuideConfig config; GuideSessionStore sessions; GuideChatService chat; GuideModelClient client; MockMvc mvc;
    ObjectMapper json = new ObjectMapper();
    @BeforeEach void setup() {
        config = new GuideConfig(); config.setEnabled(true); config.setApiKey("test-placeholder");
        sessions = new GuideSessionStore(config);
        client = mock(GuideModelClient.class);
        when(client.provider()).thenReturn("deepseek");
        var router = new GuideModelRouter(config, List.of(client));
        var answers = mock(com.artiv.guide.tool.GuideAnswerRunner.class);
        when(answers.answer(any(), any(), any(), any(), any(), any())).thenAnswer(i ->
                i.<GuideModelClient>getArgument(0).stream(new GuideModelClient.Request(i.getArgument(1), false, List.of(), false), i.getArgument(2), i.getArgument(5)));
        chat = new GuideChatService(config, router, sessions, answers);
        var jwt = mock(JwtUtil.class); var mapper = mock(UserMapper.class);
        when(jwt.validate(anyString())).thenReturn(true);
        when(jwt.parseUserId("a")).thenReturn(1L); when(jwt.parseUserId("b")).thenReturn(2L);
        for (long id : List.of(1L, 2L)) {
            var user = new User(); user.setId(id); user.setRole(3); user.setStatus(0); user.setDeleted(0);
            when(mapper.selectById(id)).thenReturn(user);
        }
        mvc = MockMvcBuilders.standaloneSetup(new AdminGuideController(new GuideService(config, router), chat, sessions))
                .setControllerAdvice(new GlobalExceptionHandler()).addInterceptors(new AuthInterceptor(jwt, mapper)).build();
    }
    @AfterEach void close() { chat.close(); UserContext.clear(); }
    String payload(String id) throws Exception { return json.writeValueAsString(Map.of("clientMessageId", id, "text", "测试")); }
    String endpoint(String session) { return "/api/v1/admin/guide/sessions/" + session + "/messages/stream"; }
    void immediateReply() {
        when(client.stream(any(), any(), any())).thenAnswer(invocation -> {
            assertNull(UserContext.get(), "异步线程不应依赖请求 ThreadLocal");
            invocation.<Consumer<String>>getArgument(1).accept("你好");
            return new GuideModelClient.Reply("你好", List.of(), new GuideModelClient.Usage(10L, 2L), "stop", 1);
        });
    }
    @Test void realSseEnvelopePersistsHistoryAndDeduplicates() throws Exception {
        immediateReply();
        var session = sessions.create(1).id(); var id = UUID.randomUUID().toString();
        var pending = mvc.perform(post(endpoint(session)).header("Authorization", "Bearer a").contentType("application/json").content(payload(id)))
                .andExpect(request().asyncStarted()).andReturn();
        pending.getAsyncResult(3000);
        var response = mvc.perform(asyncDispatch(pending)).andExpect(status().isOk()).andReturn().getResponse();
        assertTrue(response.getContentType().startsWith("text/event-stream"));
        String body = response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(body.contains("event:meta") && body.contains("event:delta") && body.contains("event:done") && body.contains("你好"));
        assertEquals("complete", sessions.get(1, session).messages().get(1).status());
        mvc.perform(post(endpoint(session)).header("Authorization", "Bearer a").contentType("application/json").content(payload(id)))
                .andExpect(jsonPath("$.code").value(409));
        verify(client, times(1)).stream(any(), any(), any());
    }
    @Test void otherAdminCannotReadClearSendOrCancelOwnedSession() throws Exception {
        var session = sessions.create(1).id(); var id = UUID.randomUUID().toString();
        String path = "/api/v1/admin/guide/sessions/" + session;
        mvc.perform(get(path).header("Authorization", "Bearer b")).andExpect(jsonPath("$.code").value(404));
        mvc.perform(delete(path).header("Authorization", "Bearer b")).andExpect(jsonPath("$.code").value(404));
        mvc.perform(post(endpoint(session)).header("Authorization", "Bearer b").contentType("application/json").content(payload(id))).andExpect(jsonPath("$.code").value(404));
        mvc.perform(post(path + "/messages/" + id + "/cancel").header("Authorization", "Bearer b")).andExpect(jsonPath("$.code").value(404));
        verify(client, never()).stream(any(), any(), any());
    }
    @Test void explicitCancelStopsRunAndReleasesOwnerSlot() throws Exception {
        var started = new CountDownLatch(1);
        when(client.stream(any(), any(), any())).thenAnswer(invocation -> { started.countDown(); new CountDownLatch(1).await(); return null; });
        var session = sessions.create(1).id(); var id = UUID.randomUUID().toString();
        var pending = mvc.perform(post(endpoint(session)).header("Authorization", "Bearer a").contentType("application/json").content(payload(id))).andReturn();
        assertTrue(started.await(3, TimeUnit.SECONDS));
        mvc.perform(post(endpoint(session)).header("Authorization", "Bearer a").contentType("application/json").content(payload(UUID.randomUUID().toString())))
                .andExpect(jsonPath("$.code").value(409));
        mvc.perform(post("/api/v1/admin/guide/sessions/" + session + "/messages/" + id + "/cancel").header("Authorization", "Bearer a"))
                .andExpect(jsonPath("$.code").value(200));
        pending.getAsyncResult(3000);
        assertEquals("cancelled", sessions.get(1, session).messages().get(1).status());
        assertNull(sessions.get(1, session).activeMessageId());
    }
    @Test void disabledFeatureAndInvalidTextRejectBeforeStreaming() throws Exception {
        var session = sessions.create(1).id(); config.setEnabled(false);
        mvc.perform(post(endpoint(session)).header("Authorization", "Bearer a").contentType("application/json").content(payload(UUID.randomUUID().toString())))
                .andExpect(jsonPath("$.code").value(503));
        config.setEnabled(true);
        mvc.perform(post(endpoint(session)).header("Authorization", "Bearer a").contentType("application/json").content("{\"clientMessageId\":\"bad\",\"text\":\"\"}"))
                .andExpect(jsonPath("$.code").value(2001));
        verify(client, never()).stream(any(), any(), any());
    }
}
