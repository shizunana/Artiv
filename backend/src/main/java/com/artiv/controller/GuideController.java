package com.artiv.controller;

import com.artiv.common.Result;
import com.artiv.dto.guide.GuideCapabilities;
import com.artiv.service.GuideService;
import com.artiv.service.GuideChatService;
import com.artiv.guide.session.GuideSessionStore;
import com.artiv.dto.guide.GuideMessageRequest;
import com.artiv.security.UserContext;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 正常登录账号的向导入口；旧管理员路径保留，所有会话操作复验本人归属。 */
@RestController
@RequestMapping({"/api/v1/guide", "/api/v1/admin/guide"})
public class GuideController {
    private final GuideService guideService;
    private final GuideChatService chat;
    private final GuideSessionStore sessions;

    public GuideController(GuideService guideService, GuideChatService chat, GuideSessionStore sessions) {
        this.guideService = guideService;
        this.chat = chat;
        this.sessions = sessions;
    }

    @GetMapping("/capabilities")
    public Result<GuideCapabilities> capabilities() {
        return Result.ok(guideService.capabilities());
    }

    @PostMapping("/sessions")
    public Result<GuideSessionStore.Snapshot> create() {
        chat.requireReady();
        return Result.ok(sessions.create(UserContext.getUserId()));
    }
    @GetMapping("/sessions/{id}")
    public Result<GuideSessionStore.Snapshot> history(@PathVariable String id) {
        return Result.ok(sessions.get(UserContext.getUserId(), id));
    }
    @DeleteMapping("/sessions/{id}")
    public Result<Void> clear(@PathVariable String id) {
        sessions.delete(UserContext.getUserId(), id);
        return Result.ok();
    }
    @PostMapping("/sessions/{id}/messages/stream")
    public SseEmitter stream(@PathVariable String id, @Valid @RequestBody GuideMessageRequest request, HttpServletResponse response) {
        var emitter = chat.stream(UserContext.getUserId(), id, request);
        response.setHeader("Cache-Control", "no-cache, no-store");
        response.setHeader("X-Accel-Buffering", "no");
        return emitter;
    }
    @PostMapping("/sessions/{id}/messages/{clientMessageId}/cancel")
    public Result<Void> cancel(@PathVariable String id, @PathVariable String clientMessageId) {
        sessions.cancel(UserContext.getUserId(), id, clientMessageId);
        return Result.ok();
    }
}
