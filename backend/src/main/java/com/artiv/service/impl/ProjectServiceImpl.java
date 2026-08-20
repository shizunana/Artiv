package com.artiv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.PublisherSummary;
import com.artiv.dto.ProjectRequest;
import com.artiv.entity.Project;
import com.artiv.entity.Invitation;
import com.artiv.entity.OrderInfo;
import com.artiv.entity.User;
import com.artiv.mapper.InvitationMapper;
import com.artiv.mapper.ApplicationMapper;
import com.artiv.entity.Application;
import com.artiv.mapper.OrderInfoMapper;
import com.artiv.mapper.ProjectMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.service.ProjectService;
import com.artiv.service.AccountRestrictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DEFAULT_PHASES = "[{\"name\":\"初稿\"},{\"name\":\"修改\"},{\"name\":\"终稿\"}]";

    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final InvitationMapper invitationMapper;
    private final ApplicationMapper applicationMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final AccountRestrictionService accountRestrictionService;

    @Override
    public Page<Project> listProjects(long page, long size, String keyword, Integer status,
                                      String sort, BigDecimal budgetMin, BigDecimal budgetMax,
                                      Integer deadlineWithin, Integer deadlineAfter, String tag,
                                      String category, String subject) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<Project>()
                .eq(Project::getDeleted, 0)
                .eq(Project::getMode, 0)
                .eq(Project::getStatus, 0)
                .eq(Project::getModerationStatus, 0)
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
        accountRestrictionService.requireAvailable(employerId, "发布企划");
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
        project.setRequirements(serializeRequirements(request));
        project.setPhases(normalizePhases(request.getPhases()));
        project.setMode(request.getMode() == null ? 0 : request.getMode());
        // 招募期默认发布后 10 日
        project.setRecruitDeadline(now.plusDays(10));
        project.setStatus(0);
        project.setModerationStatus(0);
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

    private String serializeRequirements(ProjectRequest request) {
        if (request.getRequirements() == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(request.getRequirements());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize project requirements", e);
        }
    }

    @Override
    public Project getById(Long id, Long viewerId) {
        Project project = projectMapper.selectById(id);
        if (project == null || project.getDeleted() == 1) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        boolean active = Integer.valueOf(0).equals(project.getStatus())
                && !Integer.valueOf(1).equals(project.getModerationStatus());
        boolean owner = viewerId != null && viewerId.equals(project.getEmployerId());
        boolean participant = viewerId != null && orderInfoMapper.selectCount(
                new LambdaQueryWrapper<OrderInfo>()
                        .eq(OrderInfo::getProjectId, id)
                        .and(wrapper -> wrapper.eq(OrderInfo::getEmployerId, viewerId)
                                .or().eq(OrderInfo::getPainterId, viewerId))) > 0;
        boolean existingApplicant = viewerId != null && Integer.valueOf(1).equals(project.getModerationStatus())
                && applicationMapper.selectCount(new LambdaQueryWrapper<Application>()
                .eq(Application::getProjectId, id)
                .eq(Application::getPainterId, viewerId)
                .eq(Application::getDeleted, 0)) > 0;
        if (!active && !owner && !participant && !existingApplicant) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        project.setEmployer(toPublisherSummary(userMapper.selectById(project.getEmployerId())));
        return project;
    }

    @Override
    public Project changeStatus(Long id, Integer status, Long employerId) {
        if (status == null || (status != 0 && status != 4)) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
        Project project = requireOwned(id, employerId);
        if (Integer.valueOf(1).equals(project.getModerationStatus())) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "违规下架企划只能由管理员恢复");
        }
        project.setStatus(status);
        if (status == 0 && (project.getRecruitDeadline() == null
                || project.getRecruitDeadline().isBefore(LocalDateTime.now()))) {
            project.setRecruitDeadline(LocalDateTime.now().plusDays(10));
        }
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);
        return project;
    }

    @Override
    public void delete(Long id, Long employerId) {
        Project project = requireOwned(id, employerId);
        long relatedOrders = orderInfoMapper.selectCount(
                new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getProjectId, id));
        if (relatedOrders > 0) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(),
                    "该企划存在关联订单，不能删除；可以先下架企划");
        }
        project.setStatus(4);
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);
        projectMapper.deleteById(id);
    }

    private Project requireOwned(Long id, Long employerId) {
        Project project = projectMapper.selectById(id);
        if (project == null || Integer.valueOf(1).equals(project.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (employerId == null || !employerId.equals(project.getEmployerId())) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
        return project;
    }

    private String normalizePhases(String value) {
        if (!StringUtils.hasText(value)) {
            return DEFAULT_PHASES;
        }
        try {
            List<Map<String, Object>> source = OBJECT_MAPPER.readValue(value,
                    new com.fasterxml.jackson.core.type.TypeReference<>() {});
            if (source == null || source.isEmpty() || source.size() > 10) {
                throw new IllegalArgumentException();
            }
            List<Map<String, String>> normalized = source.stream().map(item -> {
                String name = String.valueOf(item.getOrDefault("name", "")).trim();
                if (!StringUtils.hasText(name) || name.length() > 20) {
                    throw new IllegalArgumentException();
                }
                Map<String, String> phase = new LinkedHashMap<>();
                phase.put("name", name);
                return phase;
            }).toList();
            return OBJECT_MAPPER.writeValueAsString(normalized);
        } catch (Exception exception) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(),
                    "创作阶段格式错误，请设置 1 至 10 个有效阶段");
        }
    }

    private PublisherSummary toPublisherSummary(User user) {
        if (user == null) {
            return null;
        }
        return new PublisherSummary(
                user.getId(),
                user.getNickname(),
                user.getAvatar(),
                user.getBio(),
                user.getTags()
        );
    }
}
