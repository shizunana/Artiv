package com.huiyue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.common.BusinessException;
import com.huiyue.common.ResultCode;
import com.huiyue.dto.ApplicantPainterSummary;
import com.huiyue.dto.ApplicationDetail;
import com.huiyue.entity.Application;
import com.huiyue.entity.Project;
import com.huiyue.entity.User;
import com.huiyue.entity.Work;
import com.huiyue.mapper.ApplicationMapper;
import com.huiyue.mapper.ProjectMapper;
import com.huiyue.mapper.UserMapper;
import com.huiyue.mapper.WorkMapper;
import com.huiyue.service.ApplicationService;
import com.huiyue.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationMapper applicationMapper;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final WorkMapper workMapper;
    private final MessageService messageService;

    @Override
    public Page<ApplicationDetail> listApplicantDetails(long page, long size, Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null || !Integer.valueOf(0).equals(project.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        Page<Application> applications = applicationMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Application>()
                        .eq(Application::getDeleted, 0)
                        .eq(Application::getProjectId, projectId)
                        .in(Application::getStatus, 0, 1)
                        .orderByDesc(Application::getCreatedAt));
        Page<ApplicationDetail> result = new Page<>(page, size, applications.getTotal());
        result.setRecords(applications.getRecords().stream().map(application -> {
            ApplicationDetail detail = new ApplicationDetail();
            detail.setApplication(application);
            detail.setPainter(toPainterSummary(userMapper.selectById(application.getPainterId())));
            List<Work> works = workMapper.selectList(new LambdaQueryWrapper<Work>()
                    .eq(Work::getUserId, application.getPainterId())
                    .eq(Work::getDeleted, 0)
                    .eq(Work::getVisibility, 1)
                    .orderByDesc(Work::getCreatedAt)
                    .last("LIMIT 4"));
            detail.setWorks(works);
            return detail;
        }).toList());
        return result;
    }

    @Override
    public Page<Application> listMine(long page, long size, Long painterId) {
        return applicationMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Application>()
                        .eq(Application::getDeleted, 0)
                        .eq(Application::getPainterId, painterId)
                        .orderByDesc(Application::getCreatedAt));
    }

    @Override
    public Application create(Application application) {
        Project project = projectMapper.selectById(application.getProjectId());
        if (project == null || !Integer.valueOf(0).equals(project.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (!Integer.valueOf(0).equals(project.getStatus())
                || (project.getRecruitDeadline() != null && project.getRecruitDeadline().isBefore(LocalDateTime.now()))) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "该企划已停止应征");
        }
        if (project.getEmployerId().equals(application.getPainterId())) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "不能应征自己发布的企划");
        }
        User painter = userMapper.selectById(application.getPainterId());
        if (painter == null || !Integer.valueOf(0).equals(painter.getDeleted())
                || !Integer.valueOf(0).equals(painter.getStatus())
                || !Integer.valueOf(2).equals(painter.getRole())
                || !Integer.valueOf(2).equals(painter.getCertStatus())) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "仅已认证画师可以应征企划");
        }
        Long publicWorkCount = workMapper.selectCount(new LambdaQueryWrapper<Work>()
                .eq(Work::getUserId, application.getPainterId())
                .eq(Work::getVisibility, 1)
                .eq(Work::getDeleted, 0));
        if (publicWorkCount == null || publicWorkCount < 4) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "应征企划前至少需要公开 4 件作品");
        }
        if (application.getQuote() == null || application.getQuote().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "报价必须大于 0");
        }
        Long activeCount = applicationMapper.selectCount(new LambdaQueryWrapper<Application>()
                .eq(Application::getProjectId, application.getProjectId())
                .eq(Application::getPainterId, application.getPainterId())
                .eq(Application::getDeleted, 0)
                .in(Application::getStatus, 0, 1));
        if (activeCount != null && activeCount > 0) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "你已经应征过该企划");
        }

        application.setId(null);
        application.setStatus(0);
        application.setDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        application.setCreatedAt(now);
        application.setUpdatedAt(now);
        applicationMapper.insert(application);
        messageService.createNotification(
                project.getEmployerId(),
                1,
                "APPLICATION_CREATED",
                project.getId(),
                "画师 #" + application.getPainterId() + " 应征了你的企划「" + project.getTitle() + "」。"
        );
        return application;
    }

    @Override
    public Application select(Long id, Long employerId) {
        Application application = applicationMapper.selectById(id);
        if (application == null || !Integer.valueOf(0).equals(application.getDeleted())
                || !Integer.valueOf(0).equals(application.getStatus())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        Project project = projectMapper.selectById(application.getProjectId());
        if (project == null || !Integer.valueOf(0).equals(project.getDeleted())
                || !employerId.equals(project.getEmployerId())) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
        application.setStatus(1);
        application.setUpdatedAt(LocalDateTime.now());
        applicationMapper.updateById(application);
        messageService.createNotification(
                application.getPainterId(), 1, "APPLICATION_SELECTED", project.getId(),
                "企划方已选择你的应征，可进入消息中心继续沟通。"
        );
        return application;
    }

    @Override
    public Application updateQuote(Long id, Long painterId, BigDecimal quote) {
        Application application = applicationMapper.selectById(id);
        if (application == null || !Integer.valueOf(0).equals(application.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (!application.getPainterId().equals(painterId)) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
        if (!Integer.valueOf(0).equals(application.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "只有待处理应征可以更改报价");
        }
        application.setQuote(quote);
        application.setUpdatedAt(LocalDateTime.now());
        applicationMapper.updateById(application);
        return application;
    }

    @Override
    public void withdraw(Long id, Long painterId) {
        Application application = applicationMapper.selectById(id);
        if (application == null || !application.getPainterId().equals(painterId)) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
        if (!Integer.valueOf(0).equals(application.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID);
        }
        application.setStatus(3);
        application.setUpdatedAt(LocalDateTime.now());
        applicationMapper.updateById(application);
    }

    private ApplicantPainterSummary toPainterSummary(User painter) {
        if (painter == null) {
            return null;
        }
        return new ApplicantPainterSummary(
                painter.getId(), painter.getNickname(), painter.getAvatar(),
                painter.getBio(), painter.getTags()
        );
    }
}
