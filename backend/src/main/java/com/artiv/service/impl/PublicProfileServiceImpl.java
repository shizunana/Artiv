package com.artiv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.PublicProfileResponse;
import com.artiv.dto.ReviewStatistics;
import com.artiv.dto.StallSalesCount;
import com.artiv.entity.OrderInfo;
import com.artiv.entity.Project;
import com.artiv.entity.Stall;
import com.artiv.entity.User;
import com.artiv.entity.Work;
import com.artiv.mapper.OrderInfoMapper;
import com.artiv.mapper.ProjectMapper;
import com.artiv.mapper.StallMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.mapper.WorkMapper;
import com.artiv.service.PublicProfileService;
import com.artiv.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicProfileServiceImpl implements PublicProfileService {

    private static final int ORDER_COMPLETED = 3;
    private static final List<Integer> TERMINAL_ORDER_STATUSES = List.of(3, 4, 5);

    private final UserMapper userMapper;
    private final WorkMapper workMapper;
    private final StallMapper stallMapper;
    private final ProjectMapper projectMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final ReviewService reviewService;

    @Override
    public PublicProfileResponse getProfile(Long userId, String role) {
        User user = requireActiveUser(userId);
        String normalizedRole = StringUtils.hasText(role) ? role.trim().toLowerCase() : "painter";
        if (!"painter".equals(normalizedRole) && !"employer".equals(normalizedRole)) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "主页身份仅支持 painter 或 employer");
        }

        PublicProfileResponse response = new PublicProfileResponse();
        response.setId(user.getId());
        response.setNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : "艺绘用户");
        response.setAvatar(user.getAvatar());
        response.setBio(user.getBio());
        response.setTags(splitTags(user.getTags()));
        response.setCertStatus(user.getCertStatus());
        response.setPainterProfileAvailable(true);
        response.setEmployerProfileAvailable(true);
        response.setPublicWorkCount(countWorks(userId));
        response.setPublicStallCount(countStalls(userId));
        response.setPublicProjectCount(countProjects(userId));
        ReviewStatistics reviewStatistics = reviewService.getStatistics(userId, normalizedRole);
        response.setAverageScore(reviewStatistics.getAverageScore());
        response.setReviewCount(reviewStatistics.getReviewCount());

        if ("painter".equals(normalizedRole)) {
            List<OrderInfo> orders = listRoleOrders(userId, normalizedRole);
            RateStats completionStats = calculateCompletionRate(orders);
            response.setCompletionRate(completionStats.rate());
            response.setCompletionCompletedCount(completionStats.numerator());
            response.setCompletionTerminalCount(completionStats.denominator());
            RateStats onTimeStats = calculateOnTimeRate(orders);
            response.setOnTimeRate(onTimeStats.rate());
            response.setOnTimeCompletedCount(onTimeStats.numerator());
            response.setOnTimeEligibleCount(onTimeStats.denominator());
        }
        return response;
    }

    @Override
    public Page<Work> listWorks(Long userId, long page, long size) {
        requireActiveUser(userId);
        return workMapper.selectPage(new Page<>(page, normalizeSize(size, 20)),
                new LambdaQueryWrapper<Work>()
                        .eq(Work::getUserId, userId)
                        .eq(Work::getVisibility, 1)
                        .eq(Work::getDeleted, 0)
                        .orderByDesc(Work::getCreatedAt));
    }

    @Override
    public Page<Stall> listStalls(Long userId, long page, long size) {
        requireActiveUser(userId);
        Page<Stall> result = stallMapper.selectPage(new Page<>(page, normalizeSize(size, 20)),
                new LambdaQueryWrapper<Stall>()
                        .eq(Stall::getPainterId, userId)
                        .eq(Stall::getStatus, 1)
                        .eq(Stall::getDeleted, 0)
                        .gt(Stall::getStock, 0)
                        .orderByDesc(Stall::getCreatedAt));
        fillSoldCounts(result.getRecords());
        return result;
    }

    @Override
    public Page<Project> listProjects(Long userId, long page, long size) {
        requireActiveUser(userId);
        return projectMapper.selectPage(new Page<>(page, normalizeSize(size, 12)),
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getEmployerId, userId)
                        .eq(Project::getMode, 0)
                        .eq(Project::getDeleted, 0)
                        .eq(Project::getModerationStatus, 0)
                        .in(Project::getStatus, 0, 3)
                        .orderByDesc(Project::getCreatedAt));
    }

    private User requireActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())
                || !Integer.valueOf(0).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        return user;
    }

    private long countWorks(Long userId) {
        return workMapper.selectCount(new LambdaQueryWrapper<Work>()
                .eq(Work::getUserId, userId)
                .eq(Work::getVisibility, 1)
                .eq(Work::getDeleted, 0));
    }

    private long countStalls(Long userId) {
        return stallMapper.selectCount(new LambdaQueryWrapper<Stall>()
                .eq(Stall::getPainterId, userId)
                .eq(Stall::getStatus, 1)
                .eq(Stall::getDeleted, 0)
                .gt(Stall::getStock, 0));
    }

    private void fillSoldCounts(List<Stall> stalls) {
        if (stalls == null || stalls.isEmpty()) {
            return;
        }
        List<Long> stallIds = stalls.stream().map(Stall::getId).filter(java.util.Objects::nonNull).distinct().toList();
        List<StallSalesCount> summaries = stallIds.isEmpty()
                ? List.of()
                : orderInfoMapper.selectCompletedSalesByStallIds(stallIds);
        Map<Long, Long> soldCountByStall = (summaries == null ? List.<StallSalesCount>of() : summaries).stream()
                .collect(Collectors.toMap(StallSalesCount::getStallId, StallSalesCount::getSoldCount));
        stalls.forEach(stall -> stall.setSoldCount(soldCountByStall.getOrDefault(stall.getId(), 0L)));
    }

    private long countProjects(Long userId) {
        return projectMapper.selectCount(new LambdaQueryWrapper<Project>()
                .eq(Project::getEmployerId, userId)
                .eq(Project::getMode, 0)
                .eq(Project::getDeleted, 0)
                .eq(Project::getModerationStatus, 0)
                .in(Project::getStatus, 0, 3));
    }

    private List<OrderInfo> listRoleOrders(Long userId, String role) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getDeleted, 0);
        if ("painter".equals(role)) {
            wrapper.eq(OrderInfo::getPainterId, userId);
        } else {
            wrapper.eq(OrderInfo::getEmployerId, userId);
        }
        return orderInfoMapper.selectList(wrapper);
    }

    private RateStats calculateCompletionRate(List<OrderInfo> orders) {
        long concluded = orders.stream().filter(order -> TERMINAL_ORDER_STATUSES.contains(order.getStatus())).count();
        if (concluded == 0) {
            return new RateStats(null, 0, 0);
        }
        long completed = orders.stream().filter(order -> Integer.valueOf(ORDER_COMPLETED).equals(order.getStatus())).count();
        return new RateStats(percentage(completed, concluded), completed, concluded);
    }

    private RateStats calculateOnTimeRate(List<OrderInfo> orders) {
        List<OrderInfo> measurable = orders.stream()
                .filter(order -> Integer.valueOf(ORDER_COMPLETED).equals(order.getStatus()))
                .filter(order -> order.getProjectId() != null && order.getCompletedAt() != null)
                .toList();
        if (measurable.isEmpty()) {
            return new RateStats(null, 0, 0);
        }
        Map<Long, Project> projects = projectMapper.selectBatchIds(
                        measurable.stream().map(OrderInfo::getProjectId).distinct().toList())
                .stream().collect(Collectors.toMap(Project::getId, Function.identity()));
        List<OrderInfo> datedOrders = measurable.stream()
                .filter(order -> {
                    Project project = projects.get(order.getProjectId());
                    return project != null && project.getDeadline() != null;
                })
                .toList();
        if (datedOrders.isEmpty()) {
            return new RateStats(null, 0, 0);
        }
        long onTime = datedOrders.stream().filter(order -> {
            LocalDate deadline = projects.get(order.getProjectId()).getDeadline();
            return !order.getCompletedAt().toLocalDate().isAfter(deadline);
        }).count();
        return new RateStats(percentage(onTime, datedOrders.size()), onTime, datedOrders.size());
    }

    private Integer percentage(long numerator, long denominator) {
        return java.math.BigDecimal.valueOf(numerator)
                .multiply(java.math.BigDecimal.valueOf(100))
                .divide(java.math.BigDecimal.valueOf(denominator), 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private List<String> splitTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private long normalizeSize(long requested, long fallback) {
        if (requested <= 0) {
            return fallback;
        }
        return Math.min(requested, 50);
    }

    private record RateStats(Integer rate, long numerator, long denominator) {
    }
}
