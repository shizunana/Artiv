package com.artiv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.ApplicantPainterSummary;
import com.artiv.dto.ApplicationDetail;
import com.artiv.dto.MyApplicationItem;
import com.artiv.dto.ReviewStatistics;
import com.artiv.entity.Application;
import com.artiv.entity.Invitation;
import com.artiv.entity.OrderInfo;
import com.artiv.entity.Project;
import com.artiv.entity.User;
import com.artiv.entity.Work;
import com.artiv.mapper.ApplicationMapper;
import com.artiv.mapper.InvitationMapper;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationMapper applicationMapper;
    private final InvitationMapper invitationMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final WorkMapper workMapper;
    private final MessageService messageService;
    private final AccountRestrictionService accountRestrictionService;
    private final ReviewService reviewService;

    @Override
    public Page<ApplicationDetail> listApplicantDetails(long page, long size, Long projectId, Long viewerId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null || !Integer.valueOf(0).equals(project.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        requireApplicantListAccess(project, viewerId);
        Page<Application> applications = applicationMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Application>()
                        .eq(Application::getDeleted, 0)
                        .eq(Application::getProjectId, projectId)
                        .in(Application::getStatus, 0, 1)
                        .orderByDesc(Application::getCreatedAt));
        Invitation directedInvitation = null;
        if (Integer.valueOf(1).equals(project.getMode())) {
            directedInvitation = invitationMapper.selectOne(new LambdaQueryWrapper<Invitation>()
                    .eq(Invitation::getProjectId, projectId)
                    .eq(Invitation::getDeleted, 0)
                    .orderByDesc(Invitation::getCreatedAt)
                    .last("LIMIT 1"));
        }
        List<Long> painterIds = new ArrayList<>(applications.getRecords().stream()
                .map(Application::getPainterId)
                .distinct()
                .toList());
        boolean showPendingInvitation = directedInvitation != null
                && Integer.valueOf(0).equals(directedInvitation.getStatus())
                && applications.getRecords().isEmpty();
        if (showPendingInvitation && !painterIds.contains(directedInvitation.getPainterId())) {
            painterIds.add(directedInvitation.getPainterId());
        }
        Map<Long, ReviewStatistics> reviewStatistics = java.util.Optional.ofNullable(reviewService.getStatistics(
                painterIds, "painter"))
                .orElse(Map.of());
        Integer directedStatus = directedInvitation == null ? null : directedInvitation.getStatus();
        List<ApplicationDetail> records = new ArrayList<>(applications.getRecords().stream()
                .map(application -> toApplicationDetail(application, directedStatus, reviewStatistics))
                .toList());
        if (showPendingInvitation && page == 1 && size > 0) {
            records.add(toPendingInvitationDetail(directedInvitation, reviewStatistics));
        }
        long total = showPendingInvitation ? 1 : applications.getTotal();
        Page<ApplicationDetail> result = new Page<>(page, size, total);
        result.setRecords(records);
        return result;
    }

    private ApplicationDetail toApplicationDetail(Application application, Integer invitationStatus,
                                                  Map<Long, ReviewStatistics> reviewStatistics) {
        ApplicationDetail detail = new ApplicationDetail();
        detail.setApplication(application);
        detail.setInvitationStatus(invitationStatus);
        detail.setPainter(toPainterSummary(userMapper.selectById(application.getPainterId()),
                reviewStatistics.get(application.getPainterId())));
        detail.setWorks(listPublicWorks(application.getPainterId()));
        OrderInfo activeOrder = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getApplicationId, application.getId())
                .eq(OrderInfo::getDeleted, 0)
                .in(OrderInfo::getStatus, 0, 1, 2, 5)
                .orderByDesc(OrderInfo::getCreatedAt)
                .last("LIMIT 1"));
        detail.setHasActiveOrder(activeOrder != null);
        detail.setActiveOrderId(activeOrder == null ? null : activeOrder.getId());
        return detail;
    }

    private ApplicationDetail toPendingInvitationDetail(Invitation invitation,
                                                         Map<Long, ReviewStatistics> reviewStatistics) {
        ApplicationDetail detail = new ApplicationDetail();
        detail.setInvitationId(invitation.getId());
        detail.setInvitationStatus(invitation.getStatus());
        detail.setPainter(toPainterSummary(userMapper.selectById(invitation.getPainterId()),
                reviewStatistics.get(invitation.getPainterId())));
        detail.setWorks(listPublicWorks(invitation.getPainterId()));
        detail.setHasActiveOrder(false);
        return detail;
    }

    private List<Work> listPublicWorks(Long painterId) {
        return workMapper.selectList(new LambdaQueryWrapper<Work>()
                .eq(Work::getUserId, painterId)
                .eq(Work::getDeleted, 0)
                .eq(Work::getVisibility, 1)
                .orderByDesc(Work::getCreatedAt)
                .last("LIMIT 4"));
    }

    @Override
    public Page<MyApplicationItem> listMine(long page, long size, Long painterId) {
        Page<Application> applications = applicationMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Application>()
                        .eq(Application::getDeleted, 0)
                        .eq(Application::getPainterId, painterId)
                        .orderByDesc(Application::getCreatedAt));
        List<Long> projectIds = applications.getRecords().stream()
                .map(Application::getProjectId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Project> projects = projectIds.isEmpty()
                ? Map.of()
                : projectMapper.selectBatchIds(projectIds).stream()
                        .collect(Collectors.toMap(Project::getId, Function.identity()));
        Page<MyApplicationItem> result = new Page<>(page, size, applications.getTotal());
        result.setRecords(applications.getRecords().stream().map(application -> {
            Project project = projects.get(application.getProjectId());
            MyApplicationItem item = new MyApplicationItem();
            item.setId(application.getId());
            item.setProjectId(application.getProjectId());
            item.setProjectTitle(project == null ? "企划 #" + application.getProjectId() : project.getTitle());
            item.setProjectImages(project == null ? null : project.getImages());
            item.setQuote(application.getQuote());
            item.setStatus(application.getStatus());
            item.setCreatedAt(application.getCreatedAt());
            return item;
        }).toList());
        return result;
    }

    @Override
    @Transactional
    public Application create(Application application) {
        return createInternal(application, false);
    }

    @Override
    @Transactional
    public Application createFromInvitation(Long projectId, Long painterId, BigDecimal quote) {
        Application application = new Application();
        application.setProjectId(projectId);
        application.setPainterId(painterId);
        application.setIntro("接受定向邀请");
        application.setQuote(quote);
        return createInternal(application, true);
    }

    private Application createInternal(Application application, boolean fromInvitation) {
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
        if (Integer.valueOf(1).equals(project.getMode()) && !fromInvitation) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "定向企划只能由受邀画师通过邀请入口接受");
        }
        if (fromInvitation && !Integer.valueOf(1).equals(project.getMode())) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "当前企划不是定向邀请企划");
        }
        if (project.getEmployerId().equals(application.getPainterId())) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "不能应征自己发布的企划");
        }
        User painter = userMapper.selectById(application.getPainterId());
        if (painter == null || !Integer.valueOf(0).equals(painter.getDeleted())
                || !Integer.valueOf(0).equals(painter.getStatus())
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
        if (!fromInvitation && existing != null && (Integer.valueOf(0).equals(existing.getStatus())
                || Integer.valueOf(1).equals(existing.getStatus()))) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "你已经应征过该企划");
        }

        if (fromInvitation && existing != null && (Integer.valueOf(0).equals(existing.getStatus())
                || Integer.valueOf(1).equals(existing.getStatus()))) {
            existing.setQuote(application.getQuote());
            existing.setUpdatedAt(LocalDateTime.now());
            applicationMapper.updateById(existing);
            return existing;
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
        if (!fromInvitation) {
            messageService.createNotification(
                    project.getEmployerId(),
                    1,
                    "APPLICATION_CREATED",
                    project.getId(),
                    "画师 #" + application.getPainterId() + " 应征了你的企划「" + project.getTitle() + "」。"
            );
        }
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

    private void requireApplicantListAccess(Project project, Long viewerId) {
        boolean publicProject = Integer.valueOf(0).equals(project.getMode())
                && Integer.valueOf(0).equals(project.getStatus())
                && Integer.valueOf(0).equals(project.getModerationStatus());
        if (publicProject) {
            return;
        }
        if (viewerId == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        boolean owner = viewerId.equals(project.getEmployerId());
        boolean invitedPainter = invitationMapper.selectCount(new LambdaQueryWrapper<Invitation>()
                .eq(Invitation::getProjectId, project.getId())
                .eq(Invitation::getPainterId, viewerId)
                .eq(Invitation::getDeleted, 0)) > 0;
        boolean applicant = applicationMapper.selectCount(new LambdaQueryWrapper<Application>()
                .eq(Application::getProjectId, project.getId())
                .eq(Application::getPainterId, viewerId)
                .eq(Application::getDeleted, 0)) > 0;
        boolean orderParticipant = orderInfoMapper.selectCount(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getProjectId, project.getId())
                .eq(OrderInfo::getDeleted, 0)
                .and(wrapper -> wrapper.eq(OrderInfo::getEmployerId, viewerId)
                        .or().eq(OrderInfo::getPainterId, viewerId))) > 0;
        User viewer = userMapper.selectById(viewerId);
        boolean administrator = viewer != null && Integer.valueOf(3).equals(viewer.getRole())
                && Integer.valueOf(0).equals(viewer.getStatus())
                && Integer.valueOf(0).equals(viewer.getDeleted());
        if (!owner && !invitedPainter && !applicant && !orderParticipant && !administrator) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
    }
}
