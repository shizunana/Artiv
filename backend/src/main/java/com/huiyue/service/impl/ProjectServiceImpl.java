package com.huiyue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.common.BusinessException;
import com.huiyue.common.ResultCode;
import com.huiyue.dto.ProjectRequest;
import com.huiyue.entity.Project;
import com.huiyue.entity.Invitation;
import com.huiyue.entity.User;
import com.huiyue.mapper.InvitationMapper;
import com.huiyue.mapper.ProjectMapper;
import com.huiyue.mapper.UserMapper;
import com.huiyue.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final InvitationMapper invitationMapper;

    @Override
    public Page<Project> listProjects(long page, long size, String keyword, Integer status,
                                      String sort, BigDecimal budgetMin, BigDecimal budgetMax,
                                      Integer deadlineWithin, Integer deadlineAfter, String tag,
                                      String category, String subject) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<Project>()
                .eq(Project::getDeleted, 0)
                .eq(Project::getMode, 0)
                .eq(status != null, Project::getStatus, status)
                .like(StringUtils.hasText(keyword), Project::getTitle, keyword)
                .like(StringUtils.hasText(tag), Project::getTags, tag)
                .eq(StringUtils.hasText(category), Project::getCategory, category)
                .like(StringUtils.hasText(subject), Project::getSubject, subject)
                .ge(budgetMin != null, Project::getBudget, budgetMin)
                .le(budgetMax != null, Project::getBudget, budgetMax);
        if (deadlineWithin != null) {
            wrapper.le(Project::getDeadline, LocalDate.now().plusDays(deadlineWithin));
        }
        if (deadlineAfter != null) {
            wrapper.ge(Project::getDeadline, LocalDate.now().plusDays(deadlineAfter));
        }
        if ("budget".equals(sort)) {
            wrapper.orderByDesc(Project::getBudget);
        } else {
            wrapper.orderByDesc(Project::getCreatedAt);
        }
        return projectMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Page<Project> listMine(long page, long size, Long employerId) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<Project>()
                .eq(Project::getDeleted, 0)
                .eq(Project::getEmployerId, employerId)
                .orderByDesc(Project::getCreatedAt);
        return projectMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Project create(ProjectRequest request, Long employerId) {
        // 截稿日期不能早于发布后 7 天
        LocalDate minDeadline = LocalDate.now().plusDays(7);
        if (request.getDeadline().isBefore(minDeadline)) {
            throw new BusinessException(2001, "截稿日期不能早于发布后 7 天");
        }
        LocalDateTime now = LocalDateTime.now();
        Project project = new Project();
        project.setEmployerId(employerId);
        project.setTitle(request.getTitle().trim());
        project.setDescription(request.getDescription().trim());
        project.setBudget(request.getBudget());
        project.setDeadline(request.getDeadline());
        project.setTags(request.getTags() == null || request.getTags().isEmpty()
                ? null : String.join(",", request.getTags()));
        project.setCategory(request.getCategory());
        project.setSubject(request.getSubject() == null || request.getSubject().isEmpty()
                ? null : String.join(",", request.getSubject()));
        project.setImages(request.getImages());
        project.setMode(request.getMode() == null ? 0 : request.getMode());
        // 招募期默认发布后 10 日
        project.setRecruitDeadline(now.plusDays(10));
        project.setStatus(0);
        project.setDeleted(0);
        project.setCreatedAt(now);
        project.setUpdatedAt(now);
        projectMapper.insert(project);

        // 定向邀请：创建邀请记录
        if (project.getMode() == 1 && request.getInvitePainterId() != null) {
            Invitation invitation = new Invitation();
            invitation.setProjectId(project.getId());
            invitation.setPainterId(request.getInvitePainterId());
            invitation.setMessage("企划方邀请您参与「" + project.getTitle() + "」");
            invitation.setStatus(0);
            invitation.setDeleted(0);
            invitation.setCreatedAt(now);
            invitation.setUpdatedAt(now);
            invitationMapper.insert(invitation);
        }
        return project;
    }

    @Override
    public Project getById(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null || project.getDeleted() == 1) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        return project;
    }
}
