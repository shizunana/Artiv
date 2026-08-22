package com.artiv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.ApplicantPainterSummary;
import com.artiv.dto.ApplicationDetail;
import com.artiv.dto.ReviewStatistics;
import com.artiv.entity.Application;
import com.artiv.entity.OrderInfo;
import com.artiv.entity.Project;
import com.artiv.entity.User;
import com.artiv.entity.Work;
import com.artiv.mapper.ApplicationMapper;
import com.artiv.mapper.OrderInfoMapper;
import com.artiv.mapper.ProjectMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.mapper.WorkMapper;
import com.artiv.service.ApplicationService;
import com.artiv.service.MessageService;
import com.artiv.service.AccountRestrictionService;
import com.artiv.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationMapper applicationMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final WorkMapper workMapper;
    private final MessageService messageService;
    private final AccountRestrictionService accountRestrictionService;
    private final ReviewService reviewService;

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
        Map<Long, ReviewStatistics> reviewStatistics = java.util.Optional.ofNullable(reviewService.getStatistics(
                applications.getRecords().stream().map(Application::getPainterId).distinct().toList(), "painter"))
                .orElse(Map.of());
        result.setRecords(applications.getRecords().stream().map(application -> {
            ApplicationDetail detail = new ApplicationDetail();
            detail.setApplication(application);
            detail.setPainter(toPainterSummary(userMapper.selectById(application.getPainterId()),
                    reviewStatistics.get(application.getPainterId())));
            List<Work> works = workMapper.selectList(new LambdaQueryWrapper<Work>()
                    .eq(Work::getUserId, application.getPainterId())
                    .eq(Work::getDeleted, 0)
                    .eq(Work::getVisibility, 1)
                    .orderByDesc(Work::getCreatedAt)
                    .last("LIMIT 4"));
            detail.setWorks(works);
            OrderInfo activeOrder = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>()
                    .eq(OrderInfo::getApplicationId, application.getId())
                    .eq(OrderInfo::getDeleted, 0)
                    .in(OrderInfo::getStatus, 0, 1, 2, 5)
                    .orderByDesc(OrderInfo::getCreatedAt)
                    .last("LIMIT 1"));
            detail.setHasActiveOrder(activeOrder != null);
            detail.setActiveOrderId(activeOrder == null ? null : activeOrder.getId());
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
        accountRestrictionService.requireAvailable(application.getPainterId(), "应征企划");
        Project project = projectMapper.selectById(application.getProjectId());
        if (project == null || !Integer.valueOf(0).equals(project.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (Integer.valueOf(1).equals(project.getModerationStatus())) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "该企划已因违规下架，不能新增应征");
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
        Application existing = applicationMapper.selectOne(new LambdaQueryWrapper<Application>()
                .eq(Application::getProjectId, application.getProjectId())
                .eq(Application::getPainterId, application.getPainterId())
                .eq(Application::getDeleted, 0));
        if (existing != null && (Integer.valueOf(0).equals(existing.getStatus())
                || Integer.valueOf(1).equals(existing.getStatus()))) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "你已经应征过该企划");
        }

        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setIntro(application.getIntro());
            existing.setWorkLink(application.getWorkLink());
            existing.setQuote(application.getQuote());
            existing.setStatus(0);
            existing.setCreatedAt(now);
            existing.setUpdatedAt(now);
            applicationMapper.updateById(existing);
            application = existing;
        } else {
            application.setId(null);
            application.setStatus(0);
            application.setDeleted(0);
            application.setCreatedAt(now);
            application.setUpdatedAt(now);
            applicationMapper.insert(application);
        }
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

    private ApplicantPainterSummary toPainterSummary(User painter, ReviewStatistics statistics) {
        if (painter == null) {
            return null;
        }
        ApplicantPainterSummary summary = new ApplicantPainterSummary();
        summary.setId(painter.getId());
        summary.setNickname(painter.getNickname());
        summary.setAvatar(painter.getAvatar());
        summary.setBio(painter.getBio());
        summary.setTags(painter.getTags());
        summary.setAverageScore(statistics == null ? null : statistics.getAverageScore());
        summary.setReviewCount(statistics == null ? 0 : statistics.getReviewCount());
        return summary;
    }
}
