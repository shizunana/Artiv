package com.artiv.service;

import com.artiv.common.BusinessException;
import com.artiv.dto.InvitationRequest;
import com.artiv.entity.Invitation;
import com.artiv.entity.Project;
import com.artiv.entity.User;
import com.artiv.mapper.InvitationMapper;
import com.artiv.mapper.ProjectMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.service.impl.InvitationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock private InvitationMapper invitationMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private UserMapper userMapper;
    @Mock private AccountRestrictionService accountRestrictionService;

    @InjectMocks private InvitationServiceImpl invitationService;

    @Test
    void createAcceptsCertifiedPainterWithEmployerRegistrationRole() {
        InvitationRequest request = request();
        when(projectMapper.selectById(7L)).thenReturn(project());
        when(userMapper.selectById(16L)).thenReturn(painter(1, 2));
        when(invitationMapper.selectCount(any())).thenReturn(0L);

        invitationService.create(request, 1L);

        ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);
        verify(invitationMapper).insert(captor.capture());
        assertEquals(16L, captor.getValue().getPainterId());
        assertEquals(0, captor.getValue().getStatus());
    }

    @Test
    void createRejectsUncertifiedTargetEvenWhenRegisteredAsPainter() {
        when(projectMapper.selectById(7L)).thenReturn(project());
        when(userMapper.selectById(16L)).thenReturn(painter(2, 0));

        assertThrows(BusinessException.class, () -> invitationService.create(request(), 1L));
    }

    @Test
    void respondRequiresCurrentPainterCertification() {
        Invitation invitation = new Invitation();
        invitation.setId(5L);
        invitation.setPainterId(16L);
        invitation.setStatus(0);
        invitation.setDeleted(0);
        when(invitationMapper.selectById(5L)).thenReturn(invitation);
        when(userMapper.selectById(16L)).thenReturn(painter(2, 0));

        assertThrows(BusinessException.class, () -> invitationService.respond(5L, 1, 16L));
    }

    @Test
    void respondAllowsCertifiedPainterRegardlessOfRegistrationRole() {
        Invitation invitation = new Invitation();
        invitation.setId(5L);
        invitation.setPainterId(16L);
        invitation.setStatus(0);
        invitation.setDeleted(0);
        when(invitationMapper.selectById(5L)).thenReturn(invitation);
        when(userMapper.selectById(16L)).thenReturn(painter(1, 2));

        Invitation result = invitationService.respond(5L, 1, 16L);

        assertEquals(1, result.getStatus());
        verify(invitationMapper).updateById(invitation);
    }

    private InvitationRequest request() {
        InvitationRequest request = new InvitationRequest();
        request.setProjectId(7L);
        request.setPainterId(16L);
        return request;
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
        painter.setRole(role);
        painter.setCertStatus(certStatus);
        painter.setStatus(0);
        painter.setDeleted(0);
        return painter;
    }
}
