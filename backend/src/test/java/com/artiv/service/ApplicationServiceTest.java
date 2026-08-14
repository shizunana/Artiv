package com.artiv.service;

import com.artiv.common.BusinessException;
import com.artiv.entity.Application;
import com.artiv.entity.Project;
import com.artiv.entity.User;
import com.artiv.entity.Work;
import com.artiv.mapper.ApplicationMapper;
import com.artiv.mapper.ProjectMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.mapper.WorkMapper;
import com.artiv.service.impl.ApplicationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationMapper applicationMapper;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private WorkMapper workMapper;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    @Test
    void listApplicantDetailsIsVisibleToNonEmployer() {
        Project project = new Project();
        project.setId(8L);
        project.setEmployerId(1L);
        project.setDeleted(0);
        when(projectMapper.selectById(8L)).thenReturn(project);

        var applications = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<Application>(1, 10);
        applications.setRecords(java.util.List.of());
        when(applicationMapper.selectPage(any(), any())).thenReturn(applications);

        var result = applicationService.listApplicantDetails(1, 10, 8L);

        assertEquals(0, result.getRecords().size());
        verify(applicationMapper).selectPage(any(), any());
    }

    @Test
    void applicationServiceDoesNotExposeUnverifiedProjectApplicantListing() {
        assertThrows(NoSuchMethodException.class,
                () -> ApplicationService.class.getMethod("listByProject", long.class, long.class, Long.class));
    }

    @Test
    void listApplicantDetailsEnrichesEachApplicationWithPainterAndFourWorks() {
        Project project = new Project();
        project.setId(8L);
        project.setEmployerId(1L);
        project.setDeleted(0);
        when(projectMapper.selectById(8L)).thenReturn(project);
        Application application = new Application();
        application.setPainterId(2L);
        var applications = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<Application>(1, 10);
        applications.setRecords(java.util.List.of(application));
        applications.setTotal(1);
        when(applicationMapper.selectPage(any(), any())).thenReturn(applications);
        User painter = new User();
        painter.setId(2L);
        when(userMapper.selectById(2L)).thenReturn(painter);
        var works = java.util.List.of(new Work(), new Work(), new Work(), new Work());
        when(workMapper.selectList(any())).thenReturn(works);

        var result = applicationService.listApplicantDetails(1, 10, 8L);

        assertEquals(painter.getId(), result.getRecords().getFirst().getPainter().getId());
        assertEquals(4, result.getRecords().getFirst().getWorks().size());
    }

    @Test
    void createNotifiesProjectEmployer() {
        Project project = new Project();
        project.setId(8L);
        project.setEmployerId(1L);
        project.setTitle("角色海报插画");
        project.setDeleted(0);
        project.setStatus(0);
        when(projectMapper.selectById(8L)).thenReturn(project);
        User painter = new User();
        painter.setId(2L);
        painter.setRole(2);
        painter.setCertStatus(2);
        painter.setStatus(0);
        painter.setDeleted(0);
        when(userMapper.selectById(2L)).thenReturn(painter);
        when(workMapper.selectCount(any())).thenReturn(4L);
        when(applicationMapper.selectOne(any())).thenReturn(null);

        Application application = new Application();
        application.setProjectId(8L);
        application.setPainterId(2L);
        application.setQuote(java.math.BigDecimal.valueOf(500));

        applicationService.create(application);

        verify(messageService).createNotification(
                eq(1L),
                eq(1),
                eq("APPLICATION_CREATED"),
                eq(8L),
                eq("画师 #2 应征了你的企划「角色海报插画」。")
        );
    }

    @Test
    void createRejectsPainterWithFewerThanFourPublicWorks() {
        Project project = new Project();
        project.setId(8L);
        project.setEmployerId(1L);
        project.setDeleted(0);
        project.setStatus(0);
        when(projectMapper.selectById(8L)).thenReturn(project);
        User painter = new User();
        painter.setId(2L);
        painter.setRole(2);
        painter.setCertStatus(2);
        painter.setStatus(0);
        painter.setDeleted(0);
        when(userMapper.selectById(2L)).thenReturn(painter);
        when(workMapper.selectCount(any())).thenReturn(3L);

        Application application = new Application();
        application.setProjectId(8L);
        application.setPainterId(2L);
        application.setQuote(java.math.BigDecimal.valueOf(500));

        assertThrows(BusinessException.class, () -> applicationService.create(application));
    }

    @Test
    void createReusesWithdrawnApplicationInsteadOfViolatingUniqueKey() {
        Project project = new Project();
        project.setId(8L);
        project.setEmployerId(1L);
        project.setTitle("角色海报插画");
        project.setDeleted(0);
        project.setStatus(0);
        when(projectMapper.selectById(8L)).thenReturn(project);
        User painter = new User();
        painter.setId(2L);
        painter.setRole(2);
        painter.setCertStatus(2);
        painter.setStatus(0);
        painter.setDeleted(0);
        when(userMapper.selectById(2L)).thenReturn(painter);
        when(workMapper.selectCount(any())).thenReturn(4L);
        Application withdrawn = new Application();
        withdrawn.setId(12L);
        withdrawn.setProjectId(8L);
        withdrawn.setPainterId(2L);
        withdrawn.setStatus(3);
        withdrawn.setDeleted(0);
        when(applicationMapper.selectOne(any())).thenReturn(withdrawn);

        Application request = new Application();
        request.setProjectId(8L);
        request.setPainterId(2L);
        request.setIntro("重新应征");
        request.setQuote(java.math.BigDecimal.valueOf(660));

        Application reapplied = applicationService.create(request);

        assertEquals(12L, reapplied.getId());
        assertEquals(0, reapplied.getStatus());
        assertEquals("重新应征", reapplied.getIntro());
        assertEquals(0, java.math.BigDecimal.valueOf(660).compareTo(reapplied.getQuote()));
        verify(applicationMapper).updateById(withdrawn);
    }

    @Test
    void updateQuoteAllowsOnlyPendingApplicationOwner() {
        Application application = new Application();
        application.setId(12L);
        application.setPainterId(2L);
        application.setDeleted(0);
        application.setStatus(0);
        when(applicationMapper.selectById(12L)).thenReturn(application);

        var updated = applicationService.updateQuote(12L, 2L, java.math.BigDecimal.valueOf(880));

        assertEquals(0, java.math.BigDecimal.valueOf(880).compareTo(updated.getQuote()));
        verify(applicationMapper).updateById(application);
    }

    @Test
    void selectNotifiesApplicantWithoutStartingConversation() {
        Project project = new Project();
        project.setId(8L);
        project.setEmployerId(1L);
        project.setDeleted(0);
        when(projectMapper.selectById(8L)).thenReturn(project);

        Application application = new Application();
        application.setId(12L);
        application.setProjectId(8L);
        application.setPainterId(2L);
        application.setStatus(0);
        application.setDeleted(0);
        when(applicationMapper.selectById(12L)).thenReturn(application);

        applicationService.select(12L, 1L);

        verify(applicationMapper).updateById(application);
        verify(messageService).createNotification(
                eq(2L), eq(1), eq("APPLICATION_SELECTED"), eq(8L),
                eq("企划方已选择你的应征，可进入消息中心继续沟通。")
        );
    }
}
