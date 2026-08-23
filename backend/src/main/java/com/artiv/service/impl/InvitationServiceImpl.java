package com.artiv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.InvitationRequest;
import com.artiv.entity.Invitation;
import com.artiv.entity.Project;
import com.artiv.entity.User;
import com.artiv.mapper.InvitationMapper;
import com.artiv.mapper.ProjectMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.service.AccountRestrictionService;
import com.artiv.service.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService {

    private final InvitationMapper invitationMapper;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final AccountRestrictionService accountRestrictionService;

    @Override
    public Page<Invitation> listMine(long page, long size, Long userId) {
        LambdaQueryWrapper<Invitation> wrapper = new LambdaQueryWrapper<Invitation>()
                .eq(Invitation::getPainterId, userId)
                .eq(Invitation::getDeleted, 0)
                .orderByDesc(Invitation::getCreatedAt);
        return invitationMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Invitation create(InvitationRequest request, Long employerId) {
        accountRestrictionService.requireAvailable(employerId, "发送定向邀请");
        Project project = requireInvitableProject(request.getProjectId(), employerId);
        User painter = requireCertifiedPainter(request.getPainterId());
        if (painter.getId().equals(employerId)) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "不能邀请自己参与企划");
        }
        Long duplicateCount = invitationMapper.selectCount(new LambdaQueryWrapper<Invitation>()
                .eq(Invitation::getProjectId, project.getId())
                .eq(Invitation::getPainterId, painter.getId())
                .eq(Invitation::getStatus, 0)
                .eq(Invitation::getDeleted, 0));
        if (duplicateCount != null && duplicateCount > 0) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "该画师已有待处理邀请");
        }

        Invitation invitation = new Invitation();
        invitation.setProjectId(project.getId());
        invitation.setPainterId(painter.getId());
        invitation.setMessage(request.getMessage() == null || request.getMessage().isBlank()
                ? "企划方邀请您参与「" + project.getTitle() + "」"
                : request.getMessage().trim());
        invitation.setStatus(0);
        invitation.setDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        invitation.setCreatedAt(now);
        invitation.setUpdatedAt(now);
        invitationMapper.insert(invitation);
        return invitation;
    }

    @Override
    public Invitation respond(Long id, Integer status, Long painterId) {
        Invitation invitation = invitationMapper.selectById(id);
        if (invitation == null || Integer.valueOf(1).equals(invitation.getDeleted())
                || !invitation.getPainterId().equals(painterId)) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
        if (status == null || (status != 1 && status != 2)) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "邀请只能接受或拒绝");
        }
        if (invitation.getStatus() != 0) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID);
        }
        requireCertifiedPainter(painterId);
        invitation.setStatus(status);
        invitation.setUpdatedAt(LocalDateTime.now());
        invitationMapper.updateById(invitation);
        return invitation;
    }

    private Project requireInvitableProject(Long projectId, Long employerId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null || Integer.valueOf(1).equals(project.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (!project.getEmployerId().equals(employerId)) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
        if (!Integer.valueOf(1).equals(project.getMode())
                || !Integer.valueOf(0).equals(project.getStatus())
                || Integer.valueOf(1).equals(project.getModerationStatus())
                || (project.getRecruitDeadline() != null && project.getRecruitDeadline().isBefore(LocalDateTime.now()))) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "该企划当前不能发送定向邀请");
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
}
