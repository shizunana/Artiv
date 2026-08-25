package com.artiv.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.artiv.dto.ProjectRequest;
import com.artiv.dto.ProjectRequirements;
import com.artiv.entity.Project;
import com.artiv.entity.User;
import com.artiv.mapper.InvitationMapper;
import com.artiv.mapper.OrderInfoMapper;
import com.artiv.mapper.ProjectMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.mapper.ApplicationMapper;
import com.artiv.service.impl.ProjectServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private InvitationMapper invitationMapper;

    @Mock
    private OrderInfoMapper orderInfoMapper;

    @Mock
    private ApplicationMapper applicationMapper;

    @Mock
    private AccountRestrictionService accountRestrictionService;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private ProjectServiceImpl projectService;

    @Test
    void createSerializesStructuredRequirementsBeforePersistingProject() {
        ProjectRequest request = new ProjectRequest();
        request.setTitle("Project title");
        request.setDescription("Project description");
        request.setBudget(new BigDecimal("1200.00"));
        request.setDeadline(LocalDate.now().plusDays(7));
        request.setRequirements(new ProjectRequirements("3000×4000 px", "RGB", List.of("PNG", "PSD"), "可自由发挥"));
        request.setPhases("[{\"name\":\"草图\",\"percent\":40},{\"name\":\"终稿\",\"percent\":60}]");

        projectService.create(request, 1L);

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectMapper).insert(projectCaptor.capture());
        assertEquals(
                "{\"size\":\"3000×4000 px\",\"colorMode\":\"RGB\",\"formats\":[\"PNG\",\"PSD\"],\"publishPermission\":\"可自由发挥\"}",
                projectCaptor.getValue().getRequirements()
        );
        assertEquals("[{\"name\":\"草图\"},{\"name\":\"终稿\"}]", projectCaptor.getValue().getPhases());
    }

    @Test
    void createPersistsBudgetRangeAndKeepsLegacyBudgetAsMaximum() {
        ProjectRequest request = validProjectRequest();
        request.setBudget(null);
        request.setBudgetMin(new BigDecimal("300.00"));
        request.setBudgetMax(new BigDecimal("500.00"));

        projectService.create(request, 1L);

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectMapper).insert(projectCaptor.capture());
        Project persisted = projectCaptor.getValue();
        assertEquals(new BigDecimal("300.00"), persisted.getBudgetMin());
        assertEquals(new BigDecimal("500.00"), persisted.getBudgetMax());
        assertEquals(new BigDecimal("500.00"), persisted.getBudget());
    }

    @Test
    void createRejectsReversedBudgetRange() {
        ProjectRequest request = validProjectRequest();
        request.setBudget(null);
        request.setBudgetMin(new BigDecimal("800.00"));
        request.setBudgetMax(new BigDecimal("500.00"));

        assertThrows(com.artiv.common.BusinessException.class, () -> projectService.create(request, 1L));
        verify(projectMapper, never()).insert(org.mockito.ArgumentMatchers.any(Project.class));
    }

    @Test
    void getByIdExposesOnlySafePublisherSummaryForPublicDetail() throws Exception {
        Project project = new Project();
        project.setId(7L);
        project.setEmployerId(12L);
        project.setDeleted(0);
        project.setMode(0);
        project.setStatus(0);
        project.setModerationStatus(0);
        when(projectMapper.selectById(7L)).thenReturn(project);

        User publisher = new User();
        publisher.setId(12L);
        publisher.setNickname("Public publisher");
        publisher.setAvatar("https://example.com/avatar.png");
        publisher.setBio("Illustration studio");
        publisher.setTags("fantasy,game");
        publisher.setUsername("private-login");
        publisher.setPassword("private-password");
        publisher.setRole(3);
        publisher.setCertStatus(2);
        publisher.setStatus(1);
        when(userMapper.selectById(12L)).thenReturn(publisher);

        JsonNode response = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(projectService.getById(7L, null)));

        assertEquals(12L, response.path("employer").path("id").asLong());
        assertEquals("Public publisher", response.path("employer").path("nickname").asText());
        assertEquals("https://example.com/avatar.png", response.path("employer").path("avatar").asText());
        assertEquals("Illustration studio", response.path("employer").path("bio").asText());
        assertEquals("fantasy,game", response.path("employer").path("tags").asText());
        assertFalse(response.path("employer").has("username"));
        assertFalse(response.path("employer").has("password"));
        assertFalse(response.path("employer").has("role"));
        assertFalse(response.path("employer").has("certStatus"));
        assertFalse(response.path("employer").has("status"));
    }

    @Test
    void getByIdKeepsPublicDetailAvailableWhenPublisherIsMissing() throws Exception {
        Project project = new Project();
        project.setId(7L);
        project.setEmployerId(12L);
        project.setDeleted(0);
        project.setMode(0);
        project.setStatus(0);
        project.setModerationStatus(0);
        when(projectMapper.selectById(7L)).thenReturn(project);
        when(userMapper.selectById(12L)).thenReturn(null);

        JsonNode response = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(projectService.getById(7L, null)));

        assertTrue(response.has("employer"));
        assertTrue(response.path("employer").isNull());
    }

    @Test
    void directedProjectAcceptsCertifiedPainterWithEmployerRegistrationRole() {
        ProjectRequest request = validProjectRequest();
        request.setMode(1);
        request.setInvitePainterId(16L);
        User painter = new User();
        painter.setId(16L);
        painter.setRole(1);
        painter.setCertStatus(2);
        painter.setStatus(0);
        painter.setDeleted(0);
        when(userMapper.selectById(16L)).thenReturn(painter);

        projectService.create(request, 1L);

        verify(projectMapper).insert(org.mockito.ArgumentMatchers.any(Project.class));
        verify(invitationMapper).insert(org.mockito.ArgumentMatchers.any(com.artiv.entity.Invitation.class));
    }

    @Test
    void directedProjectCreatesMatchingInvitationNotification() {
        ProjectRequest request = validProjectRequest();
        request.setMode(1);
        request.setInvitePainterId(16L);
        User painter = new User();
        painter.setId(16L);
        painter.setCertStatus(2);
        painter.setStatus(0);
        painter.setDeleted(0);
        User employer = new User();
        employer.setId(1L);
        employer.setNickname("企划阿明");
        when(userMapper.selectById(16L)).thenReturn(painter);
        when(userMapper.selectById(1L)).thenReturn(employer);
        org.mockito.Mockito.doAnswer(invocation -> {
            invocation.getArgument(0, Project.class).setId(7L);
            return 1;
        }).when(projectMapper).insert(org.mockito.ArgumentMatchers.any(Project.class));

        projectService.create(request, 1L);

        verify(messageService).createNotification(
                16L, 0, "INVITATION_CREATED", 7L,
                "企划方[企划阿明]邀请您参与企划[Project title]，请进入企划详情查看并选择接受或拒绝。"
        );
    }

    @Test
    void rejectedDirectedProjectCanBePublishedAsPublic() {
        Project project = new Project();
        project.setId(9L);
        project.setEmployerId(12L);
        project.setMode(1);
        project.setStatus(4);
        project.setModerationStatus(0);
        project.setDeleted(0);
        com.artiv.entity.Invitation invitation = new com.artiv.entity.Invitation();
        invitation.setProjectId(9L);
        invitation.setStatus(2);
        invitation.setDeleted(0);
        when(projectMapper.selectById(9L)).thenReturn(project);
        when(invitationMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(invitation);

        Project result = projectService.publishPublic(9L, 12L);

        assertEquals(0, result.getMode());
        assertEquals(0, result.getStatus());
        verify(projectMapper).updateById(project);
    }

    @Test
    void directedProjectRejectsUncertifiedTargetRegardlessOfRegistrationRole() {
        ProjectRequest request = validProjectRequest();
        request.setMode(1);
        request.setInvitePainterId(16L);
        User painter = new User();
        painter.setId(16L);
        painter.setRole(2);
        painter.setCertStatus(0);
        painter.setStatus(0);
        painter.setDeleted(0);
        when(userMapper.selectById(16L)).thenReturn(painter);

        assertThrows(com.artiv.common.BusinessException.class, () -> projectService.create(request, 1L));
        verify(projectMapper, never()).insert(org.mockito.ArgumentMatchers.any(Project.class));
    }

    @Test
    void deleteRejectsProjectThatHasAnyHistoricalOrder() {
        Project project = new Project();
        project.setId(9L);
        project.setEmployerId(12L);
        project.setDeleted(0);
        when(projectMapper.selectById(9L)).thenReturn(project);
        when(orderInfoMapper.selectCount(org.mockito.ArgumentMatchers.any())).thenReturn(1L);

        assertThrows(com.artiv.common.BusinessException.class, () -> projectService.delete(9L, 12L));
    }

    @Test
    void downlistedProjectRemainsVisibleToOrderParticipant() {
        Project project = new Project();
        project.setId(9L);
        project.setEmployerId(12L);
        project.setStatus(4);
        project.setDeleted(0);
        when(projectMapper.selectById(9L)).thenReturn(project);
        when(orderInfoMapper.selectCount(org.mockito.ArgumentMatchers.any())).thenReturn(1L);

        assertEquals(9L, projectService.getById(9L, 33L).getId());
    }

    @Test
    void downlistedProjectRemainsVisibleToActiveAdministrator() {
        Project project = new Project();
        project.setId(9L);
        project.setEmployerId(12L);
        project.setStatus(0);
        project.setModerationStatus(1);
        project.setDeleted(0);
        when(projectMapper.selectById(9L)).thenReturn(project);
        User administrator = new User();
        administrator.setId(3L);
        administrator.setRole(3);
        administrator.setStatus(0);
        administrator.setDeleted(0);
        User publisher = new User();
        publisher.setId(12L);
        publisher.setDeleted(0);
        when(userMapper.selectById(3L)).thenReturn(administrator);
        when(userMapper.selectById(12L)).thenReturn(publisher);

        assertEquals(9L, projectService.getById(9L, 3L).getId());
    }

    private ProjectRequest validProjectRequest() {
        ProjectRequest request = new ProjectRequest();
        request.setTitle("Project title");
        request.setDescription("Project description");
        request.setBudget(new BigDecimal("1200.00"));
        request.setDeadline(LocalDate.now().plusDays(7));
        request.setPhases("[{\"name\":\"初稿\"},{\"name\":\"终稿\"}]");
        return request;
    }
}
