package com.huiyue.service;

import com.huiyue.common.BusinessException;
import com.huiyue.entity.Application;
import com.huiyue.entity.Project;
import com.huiyue.entity.User;
import com.huiyue.entity.Work;
import com.huiyue.mapper.ApplicationMapper;
import com.huiyue.mapper.ProjectMapper;
import com.huiyue.mapper.UserMapper;
import com.huiyue.mapper.WorkMapper;
import com.huiyue.service.impl.ApplicationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
    void listApplicantDetailsRejectsNonEmployer() {
        Project project = new Project();
        project.setId(8L);
        project.setEmployerId(1L);
        project.setDeleted(0);
        when(projectMapper.selectById(8L)).thenReturn(project);

        assertThrows(BusinessException.class,
                () -> applicationService.listApplicantDetails(1, 10, 8L, 2L));

        verify(applicationMapper, never()).selectPage(any(), any());
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

        var result = applicationService.listApplicantDetails(1, 10, 8L, 1L);

        assertEquals(painter, result.getRecords().getFirst().getPainter());
        assertEquals(4, result.getRecords().getFirst().getWorks().size());
    }

    @Test
    void createNotifiesProjectEmployer() {
        Project project = new Project();
        project.setId(8L);
        project.setEmployerId(1L);
        project.setTitle("角色海报插画");
        project.setDeleted(0);
        when(projectMapper.selectById(8L)).thenReturn(project);

        Application application = new Application();
        application.setProjectId(8L);
        application.setPainterId(2L);

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
