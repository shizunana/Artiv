package com.artiv.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.artiv.common.BusinessException;
import com.artiv.dto.InvitationAcceptRequest;
import com.artiv.dto.InvitationRejectRequest;
import com.artiv.dto.PublicInvitationCreateRequest;
import com.artiv.entity.Application;
import com.artiv.entity.Invitation;
import com.artiv.entity.Project;
import com.artiv.entity.User;
import com.artiv.mapper.InvitationMapper;
import com.artiv.mapper.ApplicationMapper;
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
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    @Mock private ApplicationMapper applicationMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private UserMapper userMapper;
    @Mock private ApplicationService applicationService;
    @Mock private MessageService messageService;
    @Mock private AccountRestrictionService accountRestrictionService;
    @Mock private UserBlockService userBlockService;

    @InjectMocks private InvitationServiceImpl invitationService;

    @Test
    void createPublicPricedInvitationStoresPriceSnapshotAndNotifiesPainter() {
        Project project = publicProject();
        when(projectMapper.selectById(7L)).thenReturn(project);
        when(userMapper.selectById(1L)).thenReturn(employer());
        when(userMapper.selectById(16L)).thenReturn(painter(1, 2));
        when(applicationMapper.selectCount(any())).thenReturn(0L);
        PublicInvitationCreateRequest request = new PublicInvitationCreateRequest();
        request.setProjectId(7L);
        request.setPainterId(16L);
        request.setInvitationType(2);
        request.setOfferedPrice(new BigDecimal("500.00"));
        request.setMessage("希望您参与角色设计");

        Invitation created = invitationService.createPublic(request, 1L);

        assertEquals(2, created.getInvitationType());
        assertEquals(0, new BigDecimal("500.00").compareTo(created.getOfferedPrice()));
        verify(invitationMapper).insert(created);
        verify(messageService).createNotification(16L, 0, "PUBLIC_PRICED_INVITATION_CREATED", 7L,
                "企划方「企划方」邀请您参与企划「公开企划」，邀请价格「¥500」，请进入企划详情查看并选择接受或拒绝。");
    }

    @Test
    void acceptRequiresCurrentPainterCertification() {
        when(invitationMapper.selectById(5L)).thenReturn(pendingInvitation());
        when(userMapper.selectById(16L)).thenReturn(painter(2, 0));

        assertThrows(BusinessException.class,
                () -> invitationService.accept(5L, acceptRequest(), 16L));

        verify(applicationService, never()).createFromInvitation(any(), any(), any(), any(), any(), any(Boolean.class));
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
        when(applicationService.createFromInvitation(7L, 16L, "接受定向邀请",
                new BigDecimal("880.00"), 0, true))
                .thenReturn(application);
        when(invitationMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        Invitation result = invitationService.accept(5L, acceptRequest(), 16L);

        assertEquals(1, result.getStatus());
        assertEquals(33L, result.getApplicationId());
        verify(applicationService).createFromInvitation(7L, 16L, "接受定向邀请",
                new BigDecimal("880.00"), 0, true);
        verify(messageService).createInvitationAcceptedConversation(
                eq(1L), eq(16L), eq(7L),
                eq("画师「Enana」接受了您的企划「定向企划」的邀请，双方已建立会话，您可以开始与画师沟通后续合作内容。")
        );
        verify(messageService).createNotification(
                eq(1L), eq(0), eq("INVITATION_ACCEPTED"), eq(7L),
                eq("画师「Enana」接受了您的企划「定向企划」的邀请，双方已建立会话，您可以开始与画师沟通后续合作内容。")
        );
    }

    @Test
    void acceptPricedPublicInvitationUsesLockedEmployerPriceWithoutPainterIntro() {
        Invitation pending = pendingInvitation();
        pending.setInvitationType(2);
        pending.setOfferedPrice(new BigDecimal("650.00"));
        Invitation accepted = pendingInvitation();
        accepted.setInvitationType(2);
        accepted.setStatus(1);
        accepted.setApplicationId(44L);
        when(invitationMapper.selectById(5L)).thenReturn(pending, accepted);
        when(userMapper.selectById(16L)).thenReturn(painter(1, 2));
        when(projectMapper.selectById(7L)).thenReturn(publicProject());
        Application application = new Application();
        application.setId(44L);
        when(applicationService.createFromInvitation(7L, 16L, "接受带价邀请",
                new BigDecimal("650.00"), 1, false)).thenReturn(application);
        when(invitationMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(2);
        InvitationAcceptRequest request = new InvitationAcceptRequest();

        Invitation result = invitationService.accept(5L, request, 16L);

        assertEquals(1, result.getStatus());
        verify(applicationService).createFromInvitation(7L, 16L, "接受带价邀请",
                new BigDecimal("650.00"), 1, false);
        verify(messageService).createNotification(eq(1L), eq(0),
                eq("PUBLIC_PRICED_INVITATION_ACCEPTED"), eq(7L), any(String.class));
    }

    @Test
    void acceptQuotePublicInvitationUsesPainterQuoteWithoutPainterIntro() {
        Invitation pending = pendingInvitation();
        pending.setInvitationType(1);
        Invitation accepted = pendingInvitation();
        accepted.setInvitationType(1);
        accepted.setStatus(1);
        accepted.setApplicationId(45L);
        when(invitationMapper.selectById(5L)).thenReturn(pending, accepted);
        when(userMapper.selectById(16L)).thenReturn(painter(1, 2));
        when(projectMapper.selectById(7L)).thenReturn(publicProject());
        Application application = new Application();
        application.setId(45L);
        when(applicationService.createFromInvitation(7L, 16L, "接受邀请报价",
                new BigDecimal("720.00"), 0, false)).thenReturn(application);
        when(invitationMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(2);
        InvitationAcceptRequest request = new InvitationAcceptRequest();
        request.setQuote(new BigDecimal("720.00"));

        Invitation result = invitationService.accept(5L, request, 16L);

        assertEquals(1, result.getStatus());
        verify(applicationService).createFromInvitation(7L, 16L, "接受邀请报价",
                new BigDecimal("720.00"), 0, false);
        verify(messageService).createNotification(eq(1L), eq(0),
                eq("PUBLIC_QUOTE_INVITATION_ACCEPTED"), eq(7L), any(String.class));
    }

    @Test
    void expiredDirectedProjectCannotBeAcceptedButCanStillBeRejected() {
        Invitation pending = pendingInvitation();
        Invitation rejected = pendingInvitation();
        rejected.setStatus(2);
        rejected.setRejectReason("已错过档期");
        Project expired = project();
        expired.setDeadline(LocalDate.now().minusDays(1));
        when(invitationMapper.selectById(5L)).thenReturn(pending, pending, rejected);
        when(userMapper.selectById(16L)).thenReturn(painter(2, 2));
        when(projectMapper.selectById(7L)).thenReturn(expired);
        when(invitationMapper.selectCount(any())).thenReturn(1L);
        when(invitationMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        assertThrows(BusinessException.class,
                () -> invitationService.accept(5L, acceptRequest(), 16L));

        InvitationRejectRequest rejection = new InvitationRejectRequest();
        rejection.setReason("已错过档期");
        Invitation result = invitationService.reject(5L, rejection, 16L);
        assertEquals(2, result.getStatus());
        verify(applicationService, never()).createFromInvitation(any(), any(), any(), any(), any(), any(Boolean.class));
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
        verify(applicationService, never()).createFromInvitation(any(), any(), any(), any(), any(), any(Boolean.class));
        verify(messageService, never()).createInvitationAcceptedConversation(any(), any(), any(), any());
        verify(messageService).createNotification(
                eq(1L), eq(0), eq("INVITATION_REJECTED"), eq(7L),
                eq("画师「Enana」婉拒了您的企划「定向企划」的邀请，婉拒理由「档期不合适」")
        );
        org.mockito.ArgumentCaptor<Project> projectCaptor = org.mockito.ArgumentCaptor.forClass(Project.class);
        verify(projectMapper).updateById(projectCaptor.capture());
        assertEquals(4, projectCaptor.getValue().getStatus());
    }

    @Test
    void listMineOnlyReturnsVisibleRemindersAndAppliesStatusFilter() {
        when(invitationMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<>(1, 10, 0));

        invitationService.listMine(1, 10, 16L, 1);

        org.mockito.ArgumentCaptor<LambdaQueryWrapper<Invitation>> queryCaptor =
                org.mockito.ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(invitationMapper).selectPage(any(Page.class), queryCaptor.capture());
        String sql = queryCaptor.getValue().getSqlSegment();
        assertTrue(sql.contains("painter_id"));
        assertTrue(sql.contains("painter_hidden"));
        assertTrue(sql.contains("status"));
        assertTrue(sql.contains("created_at"));
    }

    @Test
    void listMineRejectsUnsupportedStatusFilter() {
        assertThrows(BusinessException.class,
                () -> invitationService.listMine(1, 10, 16L, 4));

        verify(invitationMapper, never()).selectPage(any(), any());
    }

    @Test
    void hideReminderOnlyUpdatesPainterReminderFields() {
        Invitation pending = pendingInvitation();
        pending.setPainterHidden(0);
        when(invitationMapper.selectById(5L)).thenReturn(pending);
        when(invitationMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        invitationService.hideReminder(5L, 16L);

        org.mockito.ArgumentCaptor<LambdaUpdateWrapper<Invitation>> updateCaptor =
                org.mockito.ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(invitationMapper).update(isNull(), updateCaptor.capture());
        String sqlSet = updateCaptor.getValue().getSqlSet();
        assertTrue(sqlSet.contains("painter_hidden"));
        assertTrue(sqlSet.contains("painter_hidden_at"));
        assertFalse(sqlSet.contains("status="));
        assertFalse(sqlSet.contains("deleted="));
        verify(projectMapper, never()).updateById(any(Project.class));
        verify(messageService, never()).createNotification(any(), any(), any(), any(), any());
    }

    @Test
    void hideReminderCannotHideAnotherPaintersRecord() {
        when(invitationMapper.selectById(5L)).thenReturn(pendingInvitation());

        assertThrows(BusinessException.class,
                () -> invitationService.hideReminder(5L, 99L));

        verify(invitationMapper, never()).update(any(), any());
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
        invitation.setInvitationType(0);
        invitation.setPainterHidden(0);
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
        project.setDeadline(LocalDate.now().plusDays(1));
        return project;
    }

    private Project publicProject() {
        Project project = project();
        project.setTitle("公开企划");
        project.setMode(0);
        return project;
    }

    private User employer() {
        User user = new User();
        user.setId(1L);
        user.setUsername("planner");
        user.setNickname("企划方");
        user.setStatus(0);
        user.setDeleted(0);
        return user;
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
