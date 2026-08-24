package com.artiv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.InvitationAcceptRequest;
import com.artiv.dto.InvitationProjectSummary;
import com.artiv.dto.InvitationRejectRequest;
import com.artiv.entity.Application;
import com.artiv.entity.Invitation;
import com.artiv.entity.Project;
import com.artiv.entity.User;
import com.artiv.mapper.InvitationMapper;
import com.artiv.mapper.ProjectMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.service.AccountRestrictionService;
import com.artiv.service.ApplicationService;
import com.artiv.service.InvitationService;
import com.artiv.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService {

    private final InvitationMapper invitationMapper;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final ApplicationService applicationService;
    private final MessageService messageService;
    private final AccountRestrictionService accountRestrictionService;

    @Override
    public Page<InvitationProjectSummary> listMine(long page, long size, Long userId) {
        Page<Invitation> invitations = invitationMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Invitation>()
                        .eq(Invitation::getPainterId, userId)
                        .eq(Invitation::getDeleted, 0)
                        .orderByDesc(Invitation::getCreatedAt));
        List<Long> projectIds = invitations.getRecords().stream()
                .map(Invitation::getProjectId)
                .distinct()
                .toList();
        Map<Long, Project> projects = projectIds.isEmpty()
                ? Map.of()
                : projectMapper.selectBatchIds(projectIds).stream()
                        .filter(project -> Integer.valueOf(0).equals(project.getDeleted()))
                        .collect(Collectors.toMap(Project::getId, Function.identity()));
        Page<InvitationProjectSummary> result = new Page<>(page, size, invitations.getTotal());
        result.setRecords(invitations.getRecords().stream()
                .filter(invitation -> projects.containsKey(invitation.getProjectId()))
                .map(invitation -> toSummary(invitation, projects.get(invitation.getProjectId())))
                .toList());
        return result;
    }

    @Override
    public InvitationProjectSummary getMineByProject(Long projectId, Long userId) {
        Invitation invitation = invitationMapper.selectOne(new LambdaQueryWrapper<Invitation>()
                .eq(Invitation::getProjectId, projectId)
                .eq(Invitation::getPainterId, userId)
                .eq(Invitation::getDeleted, 0)
                .orderByDesc(Invitation::getCreatedAt)
                .last("LIMIT 1"));
        if (invitation == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null || !Integer.valueOf(0).equals(project.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        return toSummary(invitation, project);
    }

    @Override
    @Transactional
    public Invitation accept(Long id, InvitationAcceptRequest request, Long painterId) {
        accountRestrictionService.requireAvailable(painterId, "接受定向邀请");
        Invitation invitation = requirePendingMine(id, painterId);
        User painter = requireCertifiedPainter(painterId);
        Project project = requireActiveDirectedProject(invitation.getProjectId(), painterId);

        Application application = applicationService.createFromInvitation(project.getId(), painterId, request.getQuote());
        LocalDateTime now = LocalDateTime.now();
        int updated = invitationMapper.update(null, new LambdaUpdateWrapper<Invitation>()
                .eq(Invitation::getId, invitation.getId())
                .eq(Invitation::getPainterId, painterId)
                .eq(Invitation::getStatus, 0)
                .eq(Invitation::getDeleted, 0)
                .set(Invitation::getStatus, 1)
                .set(Invitation::getApplicationId, application.getId())
                .set(Invitation::getRejectReason, null)
                .set(Invitation::getRespondedAt, now)
                .set(Invitation::getUpdatedAt, now));
        if (updated != 1) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "邀请已被处理，请刷新后重试");
        }

        String content = "画师【" + displayName(painter) + "】接受了您的企划【" + project.getTitle()
                + "】的邀请，双方已建立会话，您可以开始与画师沟通后续合作内容。";
        messageService.createInvitationAcceptedConversation(
                project.getEmployerId(), painterId, project.getId(), content);
        messageService.createNotification(project.getEmployerId(), 0,
                "INVITATION_ACCEPTED", project.getId(), content);
        return invitationMapper.selectById(invitation.getId());
    }

    @Override
    @Transactional
    public Invitation reject(Long id, InvitationRejectRequest request, Long painterId) {
        accountRestrictionService.requireAvailable(painterId, "拒绝定向邀请");
        Invitation invitation = requirePendingMine(id, painterId);
        User painter = requireCertifiedPainter(painterId);
        Project project = requireActiveDirectedProject(invitation.getProjectId(), painterId);
        String reason = request.getReason().trim();
        LocalDateTime now = LocalDateTime.now();
        int updated = invitationMapper.update(null, new LambdaUpdateWrapper<Invitation>()
                .eq(Invitation::getId, invitation.getId())
                .eq(Invitation::getPainterId, painterId)
                .eq(Invitation::getStatus, 0)
                .eq(Invitation::getDeleted, 0)
                .set(Invitation::getStatus, 2)
                .set(Invitation::getRejectReason, reason)
                .set(Invitation::getRespondedAt, now)
                .set(Invitation::getUpdatedAt, now));
        if (updated != 1) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "邀请已被处理，请刷新后重试");
        }
        project.setStatus(4);
        project.setUpdatedAt(now);
        projectMapper.updateById(project);
        String content = "画师【" + displayName(painter) + "】婉拒了您的企划【" + project.getTitle()
                + "】的邀请，婉拒理由【" + reason + "】";
        messageService.createNotification(project.getEmployerId(), 0,
                "INVITATION_REJECTED", project.getId(), content);
        return invitationMapper.selectById(invitation.getId());
    }

    private Invitation requirePendingMine(Long id, Long painterId) {
        Invitation invitation = invitationMapper.selectById(id);
        if (invitation == null || !Integer.valueOf(0).equals(invitation.getDeleted())
                || !painterId.equals(invitation.getPainterId())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (!Integer.valueOf(0).equals(invitation.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "邀请已被处理");
        }
        return invitation;
    }

    private Project requireActiveDirectedProject(Long projectId, Long painterId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null || !Integer.valueOf(0).equals(project.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(project.getMode())
                || !Integer.valueOf(0).equals(project.getStatus())
                || Integer.valueOf(1).equals(project.getModerationStatus())
                || (project.getRecruitDeadline() != null && project.getRecruitDeadline().isBefore(LocalDateTime.now()))) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "该定向企划当前不能处理邀请");
        }
        Long targetCount = invitationMapper.selectCount(new LambdaQueryWrapper<Invitation>()
                .eq(Invitation::getProjectId, projectId)
                .eq(Invitation::getPainterId, painterId)
                .eq(Invitation::getDeleted, 0));
        if (targetCount == null || targetCount == 0) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        return project;
    }

    private User requireCertifiedPainter(Long painterId) {
        User painter = userMapper.selectById(painterId);
        if (painter == null || !Integer.valueOf(0).equals(painter.getDeleted())
                || !Integer.valueOf(0).equals(painter.getStatus())
                || !Integer.valueOf(2).equals(painter.getCertStatus())) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "仅正常状态的已认证画师可以接收和处理邀请");
        }
        return painter;
    }

    private InvitationProjectSummary toSummary(Invitation invitation, Project project) {
        InvitationProjectSummary summary = new InvitationProjectSummary();
        summary.setInvitationId(invitation.getId());
        summary.setProjectId(project.getId());
        summary.setCover(firstImage(project.getImages()));
        summary.setProjectTitle(project.getTitle());
        summary.setCategory(project.getCategory());
        summary.setBudget(project.getBudget());
        summary.setStatus(invitation.getStatus());
        summary.setRejectReason(invitation.getRejectReason());
        summary.setApplicationId(invitation.getApplicationId());
        summary.setCreatedAt(invitation.getCreatedAt());
        summary.setRespondedAt(invitation.getRespondedAt());
        return summary;
    }

    private String firstImage(String images) {
        if (images == null || images.isBlank()) {
            return null;
        }
        for (String image : images.split(",")) {
            if (!image.isBlank()) {
                return image.trim();
            }
        }
        return null;
    }

    private String displayName(User user) {
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        return user.getUsername();
    }
}
