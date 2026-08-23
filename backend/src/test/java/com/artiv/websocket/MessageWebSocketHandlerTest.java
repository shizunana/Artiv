package com.artiv.websocket;

import com.artiv.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class MessageWebSocketHandlerTest {

    @Test
    void pushIsDeliveredOnlyToTheTargetAccount() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        MessageWebSocketHandler handler = new MessageWebSocketHandler(jwtUtil);
        WebSocketSession first = session("first-token");
        WebSocketSession second = session("second-token");
        when(jwtUtil.validate("first-token")).thenReturn(true);
        when(jwtUtil.validate("second-token")).thenReturn(true);
        when(jwtUtil.parseUserId("first-token")).thenReturn(1L);
        when(jwtUtil.parseUserId("second-token")).thenReturn(2L);

        handler.afterConnectionEstablished(first);
        handler.afterConnectionEstablished(second);
        handler.pushToUser(2L, "{\"type\":\"NEW_MESSAGE\"}");

        ArgumentCaptor<TextMessage> message = ArgumentCaptor.forClass(TextMessage.class);
        verify(second).sendMessage(message.capture());
        assertEquals("{\"type\":\"NEW_MESSAGE\"}", message.getValue().getPayload());
        verify(first, never()).sendMessage(org.mockito.ArgumentMatchers.any());

        handler.afterConnectionClosed(first, CloseStatus.NORMAL);
        handler.afterConnectionClosed(second, CloseStatus.NORMAL);
    }

    @Test
    void invalidTokenCannotOpenMessageSocket() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        MessageWebSocketHandler handler = new MessageWebSocketHandler(jwtUtil);
        WebSocketSession session = session("invalid-token");
        when(jwtUtil.validate("invalid-token")).thenReturn(false);

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.POLICY_VIOLATION);
    }

    private WebSocketSession session(String token) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws?token=" + token));
        when(session.isOpen()).thenReturn(true);
        return session;
    }
}
