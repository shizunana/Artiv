package com.artiv.controller.admin;

import com.artiv.common.GlobalExceptionHandler;
import com.artiv.config.GuideConfig;
import com.artiv.entity.User;
import com.artiv.mapper.UserMapper;
import com.artiv.security.AuthInterceptor;
import com.artiv.security.JwtUtil;
import com.artiv.security.UserContext;
import com.artiv.service.GuideService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminGuideControllerTest {
    private MockMvc mvc;
    private GuideConfig config;
    private User user;

    @BeforeEach void setup() {
        config = new GuideConfig();
        var jwt = mock(JwtUtil.class);
        var mapper = mock(UserMapper.class);
        when(jwt.validate("test-token")).thenReturn(true);
        when(jwt.parseUserId("test-token")).thenReturn(8L);
        user = new User();
        user.setId(8L); user.setRole(3); user.setStatus(0); user.setDeleted(0);
        when(mapper.selectById(8L)).thenAnswer(ignored -> user);
        var router = new com.artiv.guide.client.GuideModelRouter(config, java.util.List.of(request -> null));
        mvc = MockMvcBuilders.standaloneSetup(new AdminGuideController(new GuideService(config, router), mock(com.artiv.service.GuideChatService.class), new com.artiv.guide.session.GuideSessionStore(config)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addInterceptors(new AuthInterceptor(jwt, mapper)).build();
    }
    @AfterEach void cleanup() { UserContext.clear(); }

    @Test void defaultOffAndEnabledWithoutKeyAreSafe() throws Exception {
        mvc.perform(get("/api/v1/admin/guide/capabilities").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.chatEnabled").value(false));
        config.setEnabled(true);
        mvc.perform(get("/api/v1/admin/guide/capabilities").header("Authorization", "Bearer test-token"))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.modelConfigured").value(false));
        config.setApiKey("test-secret-not-returned");
        mvc.perform(get("/api/v1/admin/guide/capabilities").header("Authorization", "Bearer test-token"))
                .andExpect(jsonPath("$.data.modelConfigured").value(true))
                .andExpect(jsonPath("$.data.imageUploadEnabled").value(true))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("test-secret"))));
    }

    @Test void anonymousAndInvalidTokenUseExisting401Envelope() throws Exception {
        mvc.perform(get("/api/v1/admin/guide/capabilities"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(401));
        mvc.perform(get("/api/v1/admin/guide/capabilities").header("Authorization", "Bearer invalid"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(401));
    }

    @Test void ordinaryDisabledDeletedAndMissingUsersAreForbidden() throws Exception {
        for (int role : new int[]{1, 2}) {
            user.setRole(role); assertForbidden();
        }
        user.setRole(3); user.setStatus(1); assertForbidden();
        user.setStatus(0); user.setDeleted(1); assertForbidden();
        user = null; assertForbidden();
    }

    private void assertForbidden() throws Exception {
        mvc.perform(get("/api/v1/admin/guide/capabilities").header("Authorization", "Bearer test-token"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(2003));
    }
}
