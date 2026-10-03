package com.artiv.guide.tool;

import com.artiv.common.BusinessException;
import com.artiv.config.GuideConfig;
import com.artiv.controller.GuideReferenceController;
import com.artiv.guide.session.GuideSessionStore;
import com.artiv.security.UserContext;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GuideReferenceTest {
    @AfterEach void clear() { UserContext.clear(); }
    @Test void onlyOwnedReturnedAndStillPublicReferencesCanNavigate() {
        var sessions = new GuideSessionStore(new GuideConfig()); var reader = mock(GuidePublicReader.class);
        var controller = new GuideReferenceController(sessions, reader);
        var session = sessions.create(1).id(); var run = sessions.begin(1, session, "test", "x", "s");
        sessions.references(run, List.of(new GuideReference("project", "11", "公开企划", "", "today"),
                new GuideReference("stall", "31", "公开橱窗", "", "today")));
        sessions.finish(run, "complete", null, null);
        login(1L);
        assertNotNull(controller.resolve(session, "project", "11"));
        verify(reader).requireVisible("project", 11L);
        assertNotNull(controller.resolve(session, "stall", "31"));
        verify(reader).requireVisible("stall", 31L);
        assertThrows(BusinessException.class, () -> controller.resolve(session, "project", "99"));
        assertThrows(BusinessException.class, () -> controller.resolve(session, "order", "11"));
        login(2L);
        assertThrows(BusinessException.class, () -> controller.resolve(session, "project", "11"));
        verifyNoMoreInteractions(reader);
        login(1L);
        doThrow(new BusinessException(404, "不可见")).when(reader).requireVisible("project", 11L);
        assertThrows(BusinessException.class, () -> controller.resolve(session, "project", "11"));
    }
    void login(long id) {
        var user = new com.artiv.security.LoginUser(); user.setId(id); user.setRole(1); UserContext.set(user);
    }
}
