package com.huiyue.service;

import com.huiyue.dto.ProjectRequest;
import com.huiyue.dto.ProjectRequirements;
import com.huiyue.entity.Project;
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
import static org.mockito.Mockito.verify;

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
                "{\"size\":\"3000×4000 px\",\"colorMode\":\"RGB\",\"deliverables\":[\"PNG\",\"PSD\"],\"notes\":\"可自由发挥\"}",
                projectCaptor.getValue().getRequirements()
        );
    }
}
