package com.artiv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.PublisherSummary;
import com.artiv.dto.ProjectRequest;
import com.artiv.dto.ProjectContentUpdateRequest;
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
import com.artiv.service.MessageService;
import com.artiv.service.UserBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.artiv.common.SystemMessageText.mark;
import static com.artiv.common.SystemMessageText.markTitle;
import static com.artiv.common.ProjectTimeRules.requireEditableDeadline;

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
    private final MessageService messageService;
    private final UserBlockService userBlockService;

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
                .isNotNull(Project::getDeadline)
                .ge(Project::getDeadline, LocalDate.now())
                .like(StringUtils.hasText(keyword), Project::getTitle, keyword)
                .like(StringUtils.hasText(tag), Project::getTags, tag)
                .eq(StringUtils.hasText(category), Project::getCategory, category)
                .like(StringUtils.hasText(subject), Project::getSubject, subject)
                .ge(budgetMin != null, Project::getBudgetMax, budgetMin)
                .le(budgetMax != null, Project::getBudgetMin, budgetMax);
        if (deadlineWithin != null) {
            wrapper.le(Project::getDeadline, LocalDate.now().plusDays(deadlineWithin));
        }
        if (deadlineAfter != null) {
            wrapper.ge(Project::getDeadline, LocalDate.now().plusDays(deadlineAfter));
        }
        if ("budget".equals(sort)) {
            wrapper.orderByDesc(Project::getBudgetMax);
        } else {
            wrapper.orderByDesc(Project::getCreatedAt);
        }
        return projectMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Page<Project> listMine(long page, long size, Long employerId, Integer mode) {
        if (mode != null && mode != 0 && mode != 1) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "合作方式不合法");
        }
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<Project>()
                .eq(Project::getDeleted, 0)
                .eq(Project::getEmployerId, employerId)
                .eq(mode != null, Project::getMode, mode)
                .orderByDesc(Project::getCreatedAt);
        Page<Project> result = projectMapper.selectPage(new Page<>(page, size), wrapper);
        attachInvitationStatuses(result.getRecords());
        attachDeleteEligibility(result.getRecords());
        return result;
    }

    @Override
    @Transactional
    public Project create(ProjectRequest request, Long employerId) {
        accountRestrictionService.requireAvailable(employerId, "发布企划");
        int mode = request.getMode() == null ? 0 : request.getMode();
        if (mode != 0 && mode != 1) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "合作方式不合法");
        }
        if (mode == 1) {
            if (request.getInvitePainterId() == null) {
                throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "定向邀请必须选择画师");
            }
            requireCertifiedPainter(request.getInvitePainterId(), employerId);
            userBlockService.requireNotBlocked(employerId, request.getInvitePainterId(), "发送邀请");
        }
        requireEditableDeadline(request.getDeadline());
        LocalDateTime now = LocalDateTime.now();
        BigDecimal[] budgetRange = resolveBudgetRange(request);
        Project project = new Project();
        project.setEmployerId(employerId);
        project.setTitle(request.getTitle().trim());
        project.setDescription(request.getDescription().trim());
        project.setBudget(budgetRange[1]);
        project.setBudgetMin(budgetRange[0]);
        project.setBudgetMax(budgetRange[1]);
        project.setDeadline(request.getDeadline());
        project.setTags(request.getTags() == null || request.getTags().isEmpty()
                ? null : String.join(",", request.getTags()));
        project.setCategory(request.getCategory());
        project.setSubject(request.getSubject() == null || request.getSubject().isEmpty()
                ? null : String.join(",", request.getSubject()));
        project.setImages(request.getImages());
        project.setRequirements(serializeRequirements(request));
        project.setPhases(normalizePhases(request.getPhases()));
        project.setMode(mode);
        project.setStatus(0);
        project.setModerationStatus(0);
        project.setDeleted(0);
        project.setCreatedAt(now);
        project.setUpdatedAt(now);
        projectMapper.insert(project);

        // 定向邀请：创建邀请记录
        if (project.getMode() == 1 && request.getInvitePainterId() != null) {
            User employer = userMapper.selectById(employerId);
            String employerName = employer == null || !StringUtils.hasText(employer.getNickname())
                    ? "企划方 #" + employerId : employer.getNickname();
            String invitationContent = "企划方" + mark(employerName) + "邀请您参与企划"
                    + markTitle(project.getTitle()) + "，请进入企划详情查看并选择接受或拒绝。";
            Invitation invitation = new Invitation();
            invitation.setProjectId(project.getId());
            invitation.setPainterId(request.getInvitePainterId());
            invitation.setMessage(invitationContent);
            invitation.setInvitationType(0);
            invitation.setStatus(0);
            invitation.setPainterHidden(0);
            invitation.setDeleted(0);
            invitation.setCreatedAt(now);
            invitation.setUpdatedAt(now);
            invitationMapper.insert(invitation);
            messageService.createNotification(request.getInvitePainterId(), 0,
                    "INVITATION_CREATED", project.getId(), invitationContent);
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
        boolean publiclyVisible = Integer.valueOf(0).equals(project.getMode())
                && Integer.valueOf(0).equals(project.getStatus())
                && Integer.valueOf(0).equals(project.getModerationStatus());
        boolean owner = viewerId != null && viewerId.equals(project.getEmployerId());
        boolean invitedPainter = viewerId != null && invitationMapper.selectCount(
                new LambdaQueryWrapper<Invitation>()
                        .eq(Invitation::getProjectId, id)
                        .eq(Invitation::getPainterId, viewerId)
                        .eq(Invitation::getDeleted, 0)) > 0;
        boolean participant = viewerId != null && orderInfoMapper.selectCount(
                new LambdaQueryWrapper<OrderInfo>()
                        .eq(OrderInfo::getProjectId, id)
                        .eq(OrderInfo::getDeleted, 0)
                        .and(wrapper -> wrapper.eq(OrderInfo::getEmployerId, viewerId)
                                .or().eq(OrderInfo::getPainterId, viewerId))) > 0;
        boolean existingApplicant = viewerId != null && applicationMapper.selectCount(
                new LambdaQueryWrapper<Application>()
                .eq(Application::getProjectId, id)
                .eq(Application::getPainterId, viewerId)
                .eq(Application::getActive, 1)
                .eq(Application::getDeleted, 0)) > 0;
        boolean administrator = false;
        if (!publiclyVisible && viewerId != null) {
            User viewer = userMapper.selectById(viewerId);
            administrator = viewer != null && Integer.valueOf(3).equals(viewer.getRole())
                    && Integer.valueOf(0).equals(viewer.getStatus())
                    && Integer.valueOf(0).equals(viewer.getDeleted());
        }
        if (!publiclyVisible && !owner && !invitedPainter && !participant && !existingApplicant && !administrator) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        project.setEmployer(toPublisherSummary(userMapper.selectById(project.getEmployerId())));
        Invitation invitation = findInvitation(project.getId());
        project.setInvitationStatus(invitation == null ? null : invitation.getStatus());
        return project;
    }

    @Override
    public Project getPanel(Long id, Long employerId) {
        Project project = requireOwned(id, employerId);
        project.setEmployer(toPublisherSummary(userMapper.selectById(project.getEmployerId())));
        Invitation invitation = findInvitation(project.getId());
        project.setInvitationStatus(invitation == null ? null : invitation.getStatus());
        project.setContentEditable(canEditContent(id));
        return project;
    }

    @Override
    @Transactional
    public Project updateContent(Long id, ProjectContentUpdateRequest request, Long employerId) {
        accountRestrictionService.requireAvailable(employerId, "编辑企划内容");
        Project project = requireOwned(id, employerId);
        if (!canEditContent(id)) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(),
                    "企划已存在应征、有效邀请或历史订单，不能修改完整内容；仍可使用变更企划调整截稿日期或上下架");
        }
        BigDecimal[] budgetRange = resolveBudgetRange(request.getBudgetMin(), request.getBudgetMax(), null);
        project.setTitle(request.getTitle().trim());
        project.setDescription(request.getDescription().trim());
        project.setBudgetMin(budgetRange[0]);
        project.setBudgetMax(budgetRange[1]);
        project.setBudget(budgetRange[1]);
        project.setTags(join(request.getTags()));
        project.setCategory(trimToNull(request.getCategory()));
        project.setSubject(join(request.getSubject()));
        project.setImages(trimToNull(request.getImages()));
        project.setRequirements(serializeRequirements(request.getRequirements()));
        project.setPhases(normalizePhases(request.getPhases()));
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);
        return project;
    }

    @Override
    public Project getGuideSummary(Long id, Long employerId) {
        Project project = requireOwned(id, employerId);
        Invitation invitation = findInvitation(id);
        project.setInvitationStatus(invitation == null ? null : invitation.getStatus());
        project.setContentEditable(canEditContent(id));
        attachDeleteEligibility(List.of(project));
        return project;
    }

    private boolean canEditContent(Long projectId) {
        long activeApplications = applicationMapper.selectCount(new LambdaQueryWrapper<Application>()
                .eq(Application::getProjectId, projectId)
                .eq(Application::getActive, 1)
                .eq(Application::getDeleted, 0));
        long activeInvitations = invitationMapper.selectCount(new LambdaQueryWrapper<Invitation>()
                .eq(Invitation::getProjectId, projectId)
                .in(Invitation::getStatus, 0, 1)
                .eq(Invitation::getDeleted, 0));
        long relatedOrders = orderInfoMapper.selectCount(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getProjectId, projectId)
                .eq(OrderInfo::getDeleted, 0));
        return activeApplications == 0 && activeInvitations == 0 && relatedOrders == 0;
    }

    private BigDecimal[] resolveBudgetRange(ProjectRequest request) {
        return resolveBudgetRange(request.getBudgetMin(), request.getBudgetMax(), request.getBudget());
    }

    private BigDecimal[] resolveBudgetRange(BigDecimal requestedMin, BigDecimal requestedMax,
                                            BigDecimal legacyBudget) {
        BigDecimal min = requestedMin;
        BigDecimal max = requestedMax;
        if (min == null && max == null && legacyBudget != null) {
            min = legacyBudget;
            max = legacyBudget;
        }
        if (min == null || max == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "请填写完整的预算区间");
        }
        if (min.signum() <= 0 || max.signum() <= 0 || min.compareTo(max) > 0) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "预算区间不合法");
        }
        return new BigDecimal[]{min, max};
    }

    private String serializeRequirements(com.artiv.dto.ProjectRequirements requirements) {
        if (requirements == null) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(requirements);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "稿件要求格式错误");
        }
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) return null;
        String value = values.stream().filter(StringUtils::hasText).map(String::trim)
                .collect(Collectors.joining(","));
        return StringUtils.hasText(value) ? value : null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private User requireCertifiedPainter(Long painterId, Long employerId) {
        if (painterId.equals(employerId)) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "不能邀请自己参与企划");
        }
        User painter = userMapper.selectById(painterId);
        if (painter == null || !Integer.valueOf(0).equals(painter.getDeleted())
                || !Integer.valueOf(0).equals(painter.getStatus())
                || !Integer.valueOf(2).equals(painter.getCertStatus())) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "只能邀请正常状态的已认证画师");
        }
        return painter;
    }

    @Override
    @Transactional
    public Project updateDeadline(Long id, LocalDate deadline, Long employerId) {
        requireEditableDeadline(deadline);
        Project project = requireOwned(id, employerId);
        if (Integer.valueOf(0).equals(project.getMode())
                && com.artiv.common.ProjectTimeRules.isExpired(project.getDeadline())) {
            invalidatePendingPublicInvitations(project.getId());
        }
        project.setDeadline(deadline);
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);
        return project;
    }

    @Override
    @Transactional
    public Project changeStatus(Long id, Integer status, LocalDate deadline, Long employerId) {
        if (status == null || (status != 0 && status != 4)) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
        Project project = requireOwned(id, employerId);
        Invitation invitation = findInvitation(id);
        if (Integer.valueOf(1).equals(project.getMode()) && invitation != null) {
            if (Integer.valueOf(0).equals(invitation.getStatus()) && status == 4) {
                throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(),
                        "待处理的定向邀请不能撤回或下架");
            }
            if (Integer.valueOf(2).equals(invitation.getStatus()) && status == 0) {
                throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(),
                        "已拒绝的定向企划只能改为公开企划");
            }
        }
        if (Integer.valueOf(1).equals(project.getModerationStatus())) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "违规下架企划只能由管理员恢复");
        }
        if (status == 0) {
            if (Integer.valueOf(0).equals(project.getMode())
                    && com.artiv.common.ProjectTimeRules.isExpired(project.getDeadline())) {
                invalidatePendingPublicInvitations(project.getId());
            }
            requireEditableDeadline(deadline);
            project.setDeadline(deadline);
        } else if (Integer.valueOf(0).equals(project.getMode())) {
            invalidatePendingPublicInvitations(project.getId());
        }
        project.setStatus(status);
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);
        return project;
    }

    @Override
    @Transactional
    public Project publishPublic(Long id, LocalDate deadline, Long employerId) {
        Project project = requireOwned(id, employerId);
        Invitation invitation = findInvitation(id);
        if (!Integer.valueOf(1).equals(project.getMode()) || invitation == null
                || !Integer.valueOf(2).equals(invitation.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(),
                    "只有已被拒绝的定向企划可以改为公开企划");
        }
        if (Integer.valueOf(1).equals(project.getModerationStatus())) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(),
                    "违规下架企划只能由管理员恢复");
        }
        requireEditableDeadline(deadline);
        project.setMode(0);
        project.setStatus(0);
        project.setDeadline(deadline);
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);
        project.setInvitationStatus(invitation.getStatus());
        return project;
    }

    @Override
    @Transactional
    public void delete(Long id, Long employerId) {
        Project project = requireOwned(id, employerId);
        Invitation invitation = findInvitation(id);
        if (Integer.valueOf(1).equals(project.getMode()) && invitation != null
                && Integer.valueOf(0).equals(invitation.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(),
                    "待处理的定向邀请不能撤回或删除企划");
        }
        long relatedOrders = orderInfoMapper.selectCount(
                new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getProjectId, id));
        if (relatedOrders > 0) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(),
                    "该企划存在关联订单，不能删除；可以先下架企划");
        }
        project.setStatus(4);
        project.setDeleted(1);
        project.setUpdatedAt(LocalDateTime.now());
        if (Integer.valueOf(0).equals(project.getMode())) {
            invalidatePendingPublicInvitations(project.getId());
        }
        projectMapper.updateById(project);
    }

    private Invitation findInvitation(Long projectId) {
        return invitationMapper.selectOne(new LambdaQueryWrapper<Invitation>()
                .eq(Invitation::getProjectId, projectId)
                .eq(Invitation::getInvitationType, 0)
                .eq(Invitation::getDeleted, 0)
                .orderByDesc(Invitation::getCreatedAt)
                .last("LIMIT 1"));
    }

    private void attachInvitationStatuses(List<Project> projects) {
        List<Long> projectIds = projects.stream()
                .filter(project -> Integer.valueOf(1).equals(project.getMode()))
                .map(Project::getId)
                .toList();
        if (projectIds.isEmpty()) {
            return;
        }
        Map<Long, Invitation> invitationByProject = invitationMapper.selectList(
                        new LambdaQueryWrapper<Invitation>()
                                .in(Invitation::getProjectId, projectIds)
                                .eq(Invitation::getInvitationType, 0)
                                .eq(Invitation::getDeleted, 0))
                .stream()
                .collect(Collectors.toMap(Invitation::getProjectId, Function.identity(),
                        (first, second) -> first.getCreatedAt().isAfter(second.getCreatedAt()) ? first : second));
        projects.forEach(project -> {
            Invitation invitation = invitationByProject.get(project.getId());
            project.setInvitationStatus(invitation == null ? null : invitation.getStatus());
        });
    }

    private void attachDeleteEligibility(List<Project> projects) {
        if (projects.isEmpty()) {
            return;
        }
        List<Long> projectIds = projects.stream().map(Project::getId).toList();
        Set<Long> projectsWithOrders = orderInfoMapper.selectList(
                        new LambdaQueryWrapper<OrderInfo>()
                                .select(OrderInfo::getProjectId)
                                .in(OrderInfo::getProjectId, projectIds))
                .stream()
                .map(OrderInfo::getProjectId)
                .collect(Collectors.toSet());

        projects.forEach(project -> {
            boolean pendingDirectedInvitation = Integer.valueOf(1).equals(project.getMode())
                    && Integer.valueOf(0).equals(project.getInvitationStatus());
            boolean hasRelatedOrder = projectsWithOrders.contains(project.getId());
            project.setDeletable(!pendingDirectedInvitation && !hasRelatedOrder);
            if (pendingDirectedInvitation && hasRelatedOrder) {
                project.setDeleteBlockReason("存在关联订单和待回应定向邀请，无法删除");
            } else if (hasRelatedOrder) {
                project.setDeleteBlockReason("存在关联订单，无法删除");
            } else if (pendingDirectedInvitation) {
                project.setDeleteBlockReason("存在待回应定向邀请，无法删除");
            } else {
                project.setDeleteBlockReason(null);
            }
        });
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

    private void invalidatePendingPublicInvitations(Long projectId) {
        LocalDateTime now = LocalDateTime.now();
        invitationMapper.update(null, new LambdaUpdateWrapper<Invitation>()
                .eq(Invitation::getProjectId, projectId)
                .in(Invitation::getInvitationType, 1, 2)
                .eq(Invitation::getStatus, 0)
                .eq(Invitation::getDeleted, 0)
                .set(Invitation::getStatus, 3)
                .set(Invitation::getRespondedAt, now)
                .set(Invitation::getUpdatedAt, now));
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
