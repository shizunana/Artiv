package com.artiv.security;

import com.artiv.common.BusinessException;
import com.artiv.entity.User;
import com.artiv.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthInterceptorTest {
    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void adminApiRejectsAuthenticatedNonAdminOnBackend() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        UserMapper userMapper = mock(UserMapper.class);
        when(jwtUtil.validate("token")).thenReturn(true);
        when(jwtUtil.parseUserId("token")).thenReturn(8L);
        User painter = new User();
        painter.setId(8L);
        painter.setRole(2);
        painter.setStatus(0);
        painter.setDeleted(0);
        when(userMapper.selectById(8L)).thenReturn(painter);

        AuthInterceptor interceptor = new AuthInterceptor(jwtUtil, userMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/overview");
        request.addHeader("Authorization", "Bearer token");

        assertThrows(BusinessException.class,
                () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }
}
