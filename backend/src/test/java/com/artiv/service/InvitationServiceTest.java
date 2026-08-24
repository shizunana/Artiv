package com.artiv.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.artiv.common.BusinessException;
import com.artiv.dto.InvitationAcceptRequest;
import com.artiv.dto.InvitationRejectRequest;
import com.artiv.entity.Application;
import com.artiv.entity.Invitation;
import com.artiv.entity.Project;
import com.artiv.entity.User;
import com.artiv.mapper.InvitationMapper;
import com.artiv.mapper.ProjectMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.service.impl.InvitationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @BeforeAll
    static void initializeInvitationLambdaMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), Invitation.class
        );
    }

    @Mock private InvitationMapper invitationMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private UserMapper userMapper;
    @Mock private ApplicationService applicationService;
    @Mock private MessageService messageService;
    @Mock private AccountRestrictionService accountRestrictionService;

    @InjectMocks private InvitationServiceImpl invitationService;

    @Test
    void acceptRequiresCurrentPainterCertification() {
        when(invitationMapper.selectById(5L)).thenReturn(pendingInvitation());
        when(userMapper.selectById(16L)).thenReturn(painter(2, 0));

        assertThrows(BusinessException.class,
                () -> invitationService.accept(5L, acceptRequest(), 16L));

        verify(applicationService, never()).createFromInvitation(any(), any(), any());
    }

    @Test
    void acceptCreatesApplicationAndConversationWithoutCreatingOrder() {
        Invitation pending = pendingInvitation();
        Invitation accepted = pendingInvitation();
        accepted.setStatus(1);
        accepted.setApplicationId(33L);
        when(invitationMapper.selectById(5L)).thenReturn(pending, accepted);
        when(userMapper.selectById(16L)).thenReturn(painter(1, 2));
        when(projectMapper.selectById(7L)).thenReturn(project());
        when(invitationMapper.selectCount(any())).thenReturn(1L);
        Application application = new Application();
        application.setId(33L);
        when(applicationService.createFromInvitation(7L, 16L, new BigDecimal("880.00")))
                .thenReturn(application);
        when(invitationMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        Invitation result = invitationService.accept(5L, acceptRequest(), 16L);

        assertEquals(1, result.getStatus());
        assertEquals(33L, result.getApplicationId());
        verify(applicationService).createFromInvitation(7L, 16L, new BigDecimal("880.00"));
        verify(messageService).createInvitationAcceptedConversation(
                eq(1L), eq(16L), eq(7L),
                eq("画师【Enana】接受了您的企划【定向企划】的邀请，双方已建立会话，您可以开始与画师沟通后续合作内容。")
        );
    }

    @Test
    void rejectOnlySendsEmployerNotificationAndDoesNotCreateApplicationOrConversation() {
        Invitation pending = pendingInvitation();
        Invitation rejected = pendingInvitation();
        rejected.setStatus(2);
        rejected.setRejectReason("档期不合适");
        when(invitationMapper.selectById(5L)).thenReturn(pending, rejected);
        when(userMapper.selectById(16L)).thenReturn(painter(2, 2));
        when(projectMapper.selectById(7L)).thenReturn(project());
        when(invitationMapper.selectCount(any())).thenReturn(1L);
        when(invitationMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        Invitation result = invitationService.reject(5L, rejectRequest(), 16L);

        assertEquals(2, result.getStatus());
        assertEquals("档期不合适", result.getRejectReason());
        verify(applicationService, never()).createFromInvitation(any(), any(), any());
        verify(messageService, never()).createInvitationAcceptedConversation(any(), any(), any(), any());
        verify(messageService).createNotification(
                eq(1L), eq(0), eq("INVITATION_REJECTED"), eq(7L),
                eq("画师【Enana】婉拒了您的企划【定向企划】的邀请，婉拒理由【档期不合适】")
        );
    }

    private InvitationAcceptRequest acceptRequest() {
        InvitationAcceptRequest request = new InvitationAcceptRequest();
        request.setQuote(new BigDecimal("880.00"));
        return request;
    }

    private InvitationRejectRequest rejectRequest() {
        InvitationRejectRequest request = new InvitationRejectRequest();
        request.setReason("档期不合适");
        return request;
    }

    private Invitation pendingInvitation() {
        Invitation invitation = new Invitation();
        invitation.setId(5L);
        invitation.setProjectId(7L);
        invitation.setPainterId(16L);
        invitation.setStatus(0);
        invitation.setDeleted(0);
        return invitation;
    }

    private Project project() {
        Project project = new Project();
        project.setId(7L);
        project.setEmployerId(1L);
        project.setTitle("定向企划");
        project.setMode(1);
        project.setStatus(0);
        project.setModerationStatus(0);
        project.setDeleted(0);
        project.setRecruitDeadline(LocalDateTime.now().plusDays(1));
        return project;
    }

    private User painter(int role, int certStatus) {
        User painter = new User();
        painter.setId(16L);
        painter.setUsername("artist16");
        painter.setNickname("Enana");
        painter.setRole(role);
        painter.setCertStatus(certStatus);
        painter.setStatus(0);
        painter.setDeleted(0);
        return painter;
    }
}
