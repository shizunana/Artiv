package com.huiyue.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息推送 WebSocket 服务端：按 userId 维护会话，支持定向推送
 */
@Slf4j
@Component
public class MessageWebSocketHandler extends TextWebSocketHandler {

    /** userId -> session */
    private static final Map<Long, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = resolveUserId(session);
        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        SESSIONS.put(userId, session);
        log.info("WebSocket 连接建立: userId={}", userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SESSIONS.entrySet().removeIf(entry -> entry.getValue().equals(session));
        log.info("WebSocket 连接关闭: {}", status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 客户端一般只收不主动发；如需双向，可在此解析 message.getPayload()
    }

    /** 向指定用户推送消息 */
    public void pushToUser(Long userId, String json) {
        WebSocketSession session = SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(json));
            } catch (Exception e) {
                log.error("推送消息失败: userId={}", userId, e);
            }
        }
    }

    /** 从 query token 解析 userId（示例；后续接入 JwtUtil 后替换） */
    private Long resolveUserId(WebSocketSession session) {
        String token = session.getUri() == null ? null : session.getUri().getQuery();
        // TODO: 使用 JwtUtil 解析 token 中的 userId
        return token == null ? null : 0L;
    }
}
