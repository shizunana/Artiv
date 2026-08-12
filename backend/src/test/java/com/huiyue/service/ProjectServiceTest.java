package com.huiyue.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huiyue.dto.ProjectRequest;
import com.huiyue.dto.ProjectRequirements;
import com.huiyue.entity.Project;
import com.huiyue.entity.User;
import com.huiyue.mapper.InvitationMapper;
import com.huiyue.mapper.ProjectMapper;
import com.huiyue.mapper.UserMapper;
import com.huiyue.service.impl.ProjectServiceImpl;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private InvitationMapper invitationMapper;

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

        projectService.create(request, 1L);

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectMapper).insert(projectCaptor.capture());
        assertEquals(
                "{\"size\":\"3000×4000 px\",\"colorMode\":\"RGB\",\"formats\":[\"PNG\",\"PSD\"],\"publishPermission\":\"可自由发挥\"}",
                projectCaptor.getValue().getRequirements()
        );
    }

    @Test
    void getByIdExposesOnlySafePublisherSummaryForPublicDetail() throws Exception {
        Project project = new Project();
        project.setId(7L);
        project.setEmployerId(12L);
        project.setDeleted(0);
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

        JsonNode response = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(projectService.getById(7L)));

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
        when(projectMapper.selectById(7L)).thenReturn(project);
        when(userMapper.selectById(12L)).thenReturn(null);

        JsonNode response = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(projectService.getById(7L)));

        assertTrue(response.has("employer"));
        assertTrue(response.path("employer").isNull());
    }
}
