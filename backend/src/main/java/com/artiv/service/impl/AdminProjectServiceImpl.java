package com.artiv.service.impl;

import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.admin.AdminProjectModerationRequest;
import com.artiv.entity.Application;
import com.artiv.entity.OrderInfo;
import com.artiv.entity.Project;
import com.artiv.entity.Invitation;
import com.artiv.entity.User;
import com.artiv.mapper.ApplicationMapper;
import com.artiv.mapper.OrderInfoMapper;
import com.artiv.mapper.ProjectMapper;
import com.artiv.mapper.InvitationMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.service.AdminAuditService;
import com.artiv.service.AdminProjectService;
import com.artiv.service.MessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.artiv.common.SystemMessageText.markTitle;

@Service
@RequiredArgsConstructor
public class AdminProjectServiceImpl implements AdminProjectService {
    private final ProjectMapper projectMapper;
    private final InvitationMapper invitationMapper;
    private final UserMapper userMapper;
    private final ApplicationMapper applicationMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final MessageService messageService;
    private final AdminAuditService auditService;

    @Override
    public Map<String, Object> list(long page, long size, Integer status, Integer moderationStatus, String keyword) {
        Page<Project> result = projectMapper.selectPage(new Page<>(page, Math.min(size, 100)),
                new LambdaQueryWrapper<Project>()
                        .eq(status != null, Project::getStatus, status)
                        .eq(moderationStatus != null, Project::getModerationStatus, moderationStatus)
                        .like(keyword != null && !keyword.isBlank(), Project::getTitle,
                                keyword == null ? null : keyword.trim())
                        .orderByDesc(Project::getCreatedAt));
        List<Map<String, Object>> records = result.getRecords().stream().map(project -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("project", project);
            User employer = userMapper.selectById(project.getEmployerId());
            if (employer != null) employer.setPassword(null);
            item.put("employer", employer);
            item.put("applicationCount", applicationMapper.selectCount(new LambdaQueryWrapper<Application>()
                    .eq(Application::getProjectId, project.getId())));
            return item;
        }).toList();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("records", records);
        response.put("total", result.getTotal());
        response.put("current", result.getCurrent());
        response.put("size", result.getSize());
        return response;
    }

    @Override
    public Map<String, Object> detail(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        User employer = userMapper.selectById(project.getEmployerId());
        if (employer != null) employer.setPassword(null);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("project", project);
        response.put("employer", employer);
        response.put("applications", applicationMapper.selectList(new LambdaQueryWrapper<Application>()
                .eq(Application::getProjectId, id).orderByDesc(Application::getCreatedAt)));
        response.put("orders", orderInfoMapper.selectList(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getProjectId, id).orderByDesc(OrderInfo::getCreatedAt)));
        return response;
    }

    @Override
    @Transactional
    public Project moderate(Long adminId, Long id, AdminProjectModerationRequest request) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        String action = request.getAction().trim().toUpperCase();
        if (!"HIDE".equals(action) && !"RESTORE".equals(action)) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "审核动作必须为 HIDE 或 RESTORE");
        }
        int target = "HIDE".equals(action) ? 1 : 0;
        if (Integer.valueOf(target).equals(project.getModerationStatus())) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "企划已经处于目标审核状态");
        }
        Project before = new Project();
        before.setId(project.getId());
        before.setModerationStatus(project.getModerationStatus());
        before.setModerationReason(project.getModerationReason());

        project.setModerationStatus(target);
        project.setModerationReason(request.getReason().trim());
        project.setModeratedBy(adminId);
        project.setModeratedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);
        if (target == 1 && Integer.valueOf(0).equals(project.getMode())) {
            LocalDateTime now = LocalDateTime.now();
            invitationMapper.update(null, new LambdaUpdateWrapper<Invitation>()
                    .eq(Invitation::getProjectId, project.getId())
                    .in(Invitation::getInvitationType, 1, 2)
                    .eq(Invitation::getStatus, 0)
                    .eq(Invitation::getDeleted, 0)
                    .set(Invitation::getStatus, 3)
                    .set(Invitation::getRespondedAt, now)
                    .set(Invitation::getUpdatedAt, now));
        }
        messageService.createNotification(project.getEmployerId(), 0,
                target == 1 ? "PROJECT_MODERATION_HIDDEN" : "PROJECT_MODERATION_RESTORED", project.getId(),
                target == 1
                        ? "您的企划" + markTitle(project.getTitle()) + "因违规已下架，现有应征与订单不受影响。原因：" + request.getReason().trim()
                        : "您的企划" + markTitle(project.getTitle()) + "已由管理员恢复展示。说明：" + request.getReason().trim());
        auditService.record(adminId, target == 1 ? "HIDE_PROJECT" : "RESTORE_PROJECT",
                "PROJECT", id, before, project, request.getReason().trim());
        return project;
    }
}
