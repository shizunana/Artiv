package com.artiv.websocket;

import lombok.extern.slf4j.Slf4j;
import com.artiv.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息推送 WebSocket 服务端：按 userId 维护会话，支持定向推送
 */
@Slf4j
@Component
public class MessageWebSocketHandler extends TextWebSocketHandler {

    /** userId -> sessions，允许同一账户多个标签页在线。 */
    private static final Map<Long, Set<WebSocketSession>> SESSIONS = new ConcurrentHashMap<>();

    private final JwtUtil jwtUtil;

    @Autowired
    public MessageWebSocketHandler(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = resolveUserId(session);
        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        SESSIONS.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
        log.info("WebSocket 连接建立: userId={}", userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SESSIONS.values().forEach(sessions -> sessions.remove(session));
        SESSIONS.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        log.info("WebSocket 连接关闭: {}", status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 客户端一般只收不主动发；如需双向，可在此解析 message.getPayload()
    }

    /** 向指定用户推送消息 */
    public void pushToUser(Long userId, String json) {
        Set<WebSocketSession> sessions = SESSIONS.get(userId);
        if (sessions == null) return;
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) continue;
            try { session.sendMessage(new TextMessage(json)); }
            catch (Exception e) { log.error("推送消息失败: userId={}", userId, e); }
        }
    }

    /** 从 query token 解析 userId（示例；后续接入 JwtUtil 后替换） */
    private Long resolveUserId(WebSocketSession session) {
        String query = session.getUri() == null ? null : session.getUri().getQuery();
        if (query == null) return null;
        for (String item : query.split("&")) {
            if (item.startsWith("token=")) {
                String token = item.substring(6);
                return jwtUtil.validate(token) ? jwtUtil.parseUserId(token) : null;
            }
        }
        return null;
    }
}
