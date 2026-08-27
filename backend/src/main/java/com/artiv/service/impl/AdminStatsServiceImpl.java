package com.artiv.service.impl;

import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.entity.AdminActionLog;
import com.artiv.entity.Application;
import com.artiv.entity.ArtistCertification;
import com.artiv.entity.OrderDispute;
import com.artiv.entity.OrderInfo;
import com.artiv.entity.Project;
import com.artiv.entity.Stall;
import com.artiv.entity.User;
import com.artiv.entity.Work;
import com.artiv.mapper.AdminActionLogMapper;
import com.artiv.mapper.ApplicationMapper;
import com.artiv.mapper.ArtistCertificationMapper;
import com.artiv.mapper.OrderDisputeMapper;
import com.artiv.mapper.OrderInfoMapper;
import com.artiv.mapper.ProjectMapper;
import com.artiv.mapper.StallMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.mapper.WorkMapper;
import com.artiv.service.AdminStatsService;
import com.artiv.service.ReportService;
import com.artiv.service.ReviewService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStatsServiceImpl implements AdminStatsService {
    private static final List<String> PROJECT_CATEGORIES = List.of(
            "头像", "Q版全身", "半身像", "立绘", "组合页",
            "服设", "壁纸", "表情包", "Live2d", "平面设计");

    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;
    private final ApplicationMapper applicationMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final StallMapper stallMapper;
    private final WorkMapper workMapper;
    private final ArtistCertificationMapper certificationMapper;
    private final OrderDisputeMapper disputeMapper;
    private final AdminActionLogMapper auditMapper;
    private final ReviewService reviewService;
    private final ReportService reportService;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> overview() {
        LocalDate today = LocalDate.now();
        Range todayRange = resolve(today, today);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalUsers", userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getDeleted, 0)));
        response.put("newUsersToday", countUsers(todayRange));
        response.put("newOrdersToday", countOrders(todayRange));
        response.put("pendingCertifications", certificationMapper.selectCount(
                new LambdaQueryWrapper<ArtistCertification>().eq(ArtistCertification::getStatus, 0)));
        response.put("pendingDisputes", disputeMapper.selectCount(
                new LambdaQueryWrapper<OrderDispute>().eq(OrderDispute::getStatus, 0)));
        response.put("pendingReviewAppeals", reviewService.countPendingAppeals());
        response.put("pendingReports", reportService.countPending());
        response.put("activeOrders", orderInfoMapper.selectCount(new LambdaQueryWrapper<OrderInfo>()
                .in(OrderInfo::getStatus, 0, 1, 2, 5).eq(OrderInfo::getDeleted, 0)));
        response.put("hiddenProjects", projectMapper.selectCount(new LambdaQueryWrapper<Project>()
                .eq(Project::getModerationStatus, 1).eq(Project::getDeleted, 0)));
        response.put("recentAuditLogs", auditMapper.selectList(new LambdaQueryWrapper<AdminActionLog>()
                .orderByDesc(AdminActionLog::getCreatedAt).last("LIMIT 8")));
        return response;
    }

    @Override
    public Map<String, Object> operations(LocalDate startDate, LocalDate endDate) {
        Range range = resolve(startDate, endDate);
        List<User> newUsers = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getDeleted, 0).ge(User::getCreatedAt, range.start()).lt(User::getCreatedAt, range.endExclusive()));
        List<OrderInfo> newOrders = orderInfoMapper.selectList(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getDeleted, 0).ge(OrderInfo::getCreatedAt, range.start()).lt(OrderInfo::getCreatedAt, range.endExclusive()));
        List<OrderInfo> completed = orderInfoMapper.selectList(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getDeleted, 0).eq(OrderInfo::getStatus, 3)
                .ge(OrderInfo::getCompletedAt, range.start()).lt(OrderInfo::getCompletedAt, range.endExclusive()));
        List<User> allUsers = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getDeleted, 0));
        List<OrderInfo> allOrders = orderInfoMapper.selectList(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getDeleted, 0));

        Map<String, Object> topMetrics = new LinkedHashMap<>();
        topMetrics.put("totalUsers", (long) allUsers.size());
        topMetrics.put("newUsers", (long) newUsers.size());
        topMetrics.put("newOrders", (long) newOrders.size());
        topMetrics.put("completedOrders", (long) completed.size());
        topMetrics.put("gmv", money(completed));

        Map<LocalDate, Long> userTrend = newUsers.stream().collect(Collectors.groupingBy(
                user -> user.getCreatedAt().toLocalDate(), Collectors.counting()));
        Map<LocalDate, Long> orderTrend = newOrders.stream().collect(Collectors.groupingBy(
                order -> order.getCreatedAt().toLocalDate(), Collectors.counting()));
        Map<LocalDate, Long> completedTrend = completed.stream().collect(Collectors.groupingBy(
                order -> order.getCompletedAt().toLocalDate(), Collectors.counting()));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("range", rangeMap(range));
        response.put("topMetrics", topMetrics);
        response.put("granularity", granularity(range));
        response.put("userTrend", timeline(range, userTrend, null));
        response.put("orderTrend", timeline(range, orderTrend, completedTrend));
        response.put("userRoles", countBy(allUsers, user -> switch (user.getRole()) {
            case 1 -> "默认企划方工作台"; case 2 -> "默认画师工作台"; case 3 -> "管理员"; default -> "未知";
        }));
        response.put("certificationStatus", countBy(allUsers, user -> switch (user.getCertStatus()) {
            case 1 -> "审核中"; case 2 -> "已认证"; case 3 -> "已驳回"; default -> "未认证";
        }));
        response.put("orderStatus", countBy(allOrders, order -> orderStatus(order.getStatus())));
        response.put("orderSource", countBy(allOrders, order -> order.getProjectId() == null ? "橱窗" : "企划"));
        response.put("topCategories", operationCategories(completed));
        return response;
    }

    @Override
    public Map<String, Object> demand(LocalDate startDate, LocalDate endDate) {
        Range range = resolve(startDate, endDate);
        List<Project> projects = projectMapper.selectList(new LambdaQueryWrapper<Project>()
                .eq(Project::getDeleted, 0).ge(Project::getCreatedAt, range.start()).lt(Project::getCreatedAt, range.endExclusive()));
        Set<Long> projectIds = projects.stream().map(Project::getId).collect(Collectors.toSet());
        List<Application> applications = projectIds.isEmpty() ? List.of() : applicationMapper.selectList(
                new LambdaQueryWrapper<Application>().in(Application::getProjectId, projectIds).eq(Application::getDeleted, 0));
        List<Application> trendApplications = applicationMapper.selectList(new LambdaQueryWrapper<Application>()
                .eq(Application::getDeleted, 0)
                .ge(Application::getCreatedAt, range.start())
                .lt(Application::getCreatedAt, range.endExclusive()));
        List<OrderInfo> orders = projectIds.isEmpty() ? List.of() : orderInfoMapper.selectList(
                new LambdaQueryWrapper<OrderInfo>().in(OrderInfo::getProjectId, projectIds).eq(OrderInfo::getDeleted, 0));

        Map<Long, List<Application>> applicationsByProject = applications.stream()
                .collect(Collectors.groupingBy(Application::getProjectId));
        Map<Long, List<OrderInfo>> ordersByProject = orders.stream()
                .collect(Collectors.groupingBy(OrderInfo::getProjectId));
        long withApplication = projects.stream().filter(p -> applicationsByProject.containsKey(p.getId())).count();
        long withOrder = projects.stream().filter(p -> ordersByProject.containsKey(p.getId())).count();
        long completed = projects.stream().filter(p -> ordersByProject.getOrDefault(p.getId(), List.of())
                .stream().anyMatch(order -> Integer.valueOf(3).equals(order.getStatus()))).count();

        Map<LocalDate, Long> projectTrend = projects.stream().collect(Collectors.groupingBy(
                p -> p.getCreatedAt().toLocalDate(), Collectors.counting()));
        Map<LocalDate, Long> appTrend = trendApplications.stream().collect(Collectors.groupingBy(
                a -> a.getCreatedAt().toLocalDate(), Collectors.counting()));
        Map<String, Long> categoryCounts = countBy(projects, p -> blankAs(p.getCategory(), "未分类"));
        Map<String, Double> categoryZeroRates = new HashMap<>();
        projects.stream().collect(Collectors.groupingBy(p -> blankAs(p.getCategory(), "未分类")))
                .forEach((category, categoryProjects) -> categoryZeroRates.put(category,
                        round(categoryProjects.stream().filter(p -> !applicationsByProject.containsKey(p.getId())).count()
                                * 100.0 / categoryProjects.size())));
        double totalApplications = applications.size();
        List<Double> firstResponseHours = new ArrayList<>();
        for (Project project : projects) {
            applicationsByProject.getOrDefault(project.getId(), List.of()).stream()
                    .map(Application::getCreatedAt).min(Comparator.naturalOrder())
                    .ifPresent(first -> firstResponseHours.add(Math.max(0, ChronoUnit.MINUTES
                            .between(project.getCreatedAt(), first) / 60.0)));
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("publishedProjects", (long) projects.size());
        summary.put("projectsWithApplications", withApplication);
        summary.put("zeroApplicationProjects", (long) projects.size() - withApplication);
        summary.put("projectsWithOrders", withOrder);
        summary.put("completedProjects", completed);
        summary.put("averageApplications", projects.isEmpty() ? 0 : round(totalApplications / projects.size()));
        summary.put("zeroApplicationRate", projects.isEmpty() ? 0 : round((projects.size() - withApplication) * 100.0 / projects.size()));
        summary.put("averageFirstResponseHours", firstResponseHours.isEmpty() ? null
                : round(firstResponseHours.stream().mapToDouble(Double::doubleValue).average().orElse(0)));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("range", rangeMap(range));
        response.put("topMetrics", basicMetrics(range));
        response.put("summary", summary);
        response.put("granularity", granularity(range));
        response.put("trend", timeline(range, projectTrend, appTrend));
        response.put("categories", sorted(categoryCounts, 20));
        response.put("funnel", List.of(
                Map.of("name", "发布企划", "value", projects.size()),
                Map.of("name", "收到应征", "value", withApplication),
                Map.of("name", "生成订单", "value", withOrder),
                Map.of("name", "完成交易", "value", completed)));
        response.put("supplyDemandGap", supplyDemandGap(categoryCounts, categoryZeroRates));
        return response;
    }

    @Override
    public Map<String, Object> priceDistribution(LocalDate startDate, LocalDate endDate, String category) {
        Range range = resolve(startDate, endDate);
        String normalizedCategory = normalizeCategory(category);
        List<OrderInfo> completedProjectOrders = orderInfoMapper.selectList(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getDeleted, 0)
                .eq(OrderInfo::getStatus, 3)
                .isNotNull(OrderInfo::getProjectId)
                .isNotNull(OrderInfo::getAmount)
                .gt(OrderInfo::getAmount, BigDecimal.ZERO)
                .ge(OrderInfo::getCompletedAt, range.start())
                .lt(OrderInfo::getCompletedAt, range.endExclusive()));

        Map<Long, String> snapshotCategories = new HashMap<>();
        Set<Long> fallbackProjectIds = new HashSet<>();
        for (OrderInfo order : completedProjectOrders) {
            String snapshotCategory = projectSnapshotCategory(order.getProjectSnapshot());
            if (snapshotCategory != null) {
                snapshotCategories.put(order.getId(), snapshotCategory);
            } else if (order.getProjectId() != null) {
                fallbackProjectIds.add(order.getProjectId());
            }
        }
        Map<Long, Project> fallbackProjects = fallbackProjectIds.isEmpty() ? Map.of()
                : projectMapper.selectBatchIds(fallbackProjectIds).stream()
                .collect(Collectors.toMap(Project::getId, Function.identity()));

        List<BigDecimal> amounts = completedProjectOrders.stream()
                .filter(order -> {
                    String orderCategory = snapshotCategories.get(order.getId());
                    if (orderCategory == null) {
                        Project project = fallbackProjects.get(order.getProjectId());
                        orderCategory = project == null ? "未分类" : blankAs(project.getCategory(), "未分类");
                    }
                    return normalizedCategory == null || normalizedCategory.equals(orderCategory);
                })
                .map(OrderInfo::getAmount)
                .sorted()
                .toList();

        return priceDistributionResponse(range, normalizedCategory, amounts);
    }

    private Map<String, Object> priceDistributionResponse(Range range, String category, List<BigDecimal> amounts) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("range", rangeMap(range));
        response.put("category", category);
        response.put("categoryLabel", category == null ? "全部类型" : category);
        response.put("sampleSize", amounts.size());
        long distinctPriceCount = amounts.stream().map(BigDecimal::stripTrailingZeros).distinct().count();
        response.put("distinctPriceCount", distinctPriceCount);

        if (amounts.isEmpty()) {
            response.put("sufficient", false);
            response.put("reason", "NO_DATA");
            response.put("buckets", List.of());
            return response;
        }

        BigDecimal total = amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        response.put("averageAmount", total.divide(BigDecimal.valueOf(amounts.size()), 2, RoundingMode.HALF_UP));
        response.put("medianAmount", median(amounts).setScale(2, RoundingMode.HALF_UP));
        response.put("minimumAmount", amounts.get(0).setScale(2, RoundingMode.HALF_UP));
        response.put("maximumAmount", amounts.get(amounts.size() - 1).setScale(2, RoundingMode.HALF_UP));

        if (amounts.size() < 10) {
            response.put("sufficient", false);
            response.put("reason", "INSUFFICIENT_SAMPLE");
            response.put("buckets", List.of());
            return response;
        }

        BigDecimal p10 = percentileBoundary(amounts, 0.10);
        BigDecimal p30 = percentileBoundary(amounts, 0.30);
        BigDecimal p70 = percentileBoundary(amounts, 0.70);
        BigDecimal p90 = percentileBoundary(amounts, 0.90);
        List<BigDecimal> boundaries = List.of(p10, p30, p70, p90);
        boolean boundariesAreDistinct = p10.compareTo(p30) < 0 && p30.compareTo(p70) < 0
                && p70.compareTo(p90) < 0;
        if (distinctPriceCount < 5 || !boundariesAreDistinct) {
            response.put("sufficient", false);
            response.put("reason", "PRICE_CONCENTRATED");
            response.put("buckets", List.of());
            return response;
        }

        String[] keys = {"LOWEST", "LOW", "MAINSTREAM", "HIGH", "HIGHEST"};
        String[] names = {"低价段", "偏低价段", "主流价格段", "偏高价段", "高价段"};
        int[] targetRates = {10, 20, 40, 20, 10};
        String[] labels = {
                "< " + currency(p10),
                currency(p10) + "–" + currency(p30),
                currency(p30) + "–" + currency(p70),
                currency(p70) + "–" + currency(p90),
                "> " + currency(p90)
        };
        int[] counts = new int[5];
        BigDecimal[] gmvs = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        for (BigDecimal amount : amounts) {
            int bucket = bucketIndex(amount, boundaries);
            counts[bucket]++;
            gmvs[bucket] = gmvs[bucket].add(amount);
        }
        List<Map<String, Object>> buckets = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            Map<String, Object> bucket = new LinkedHashMap<>();
            bucket.put("key", keys[index]);
            bucket.put("name", names[index]);
            bucket.put("rangeLabel", labels[index]);
            bucket.put("targetRate", targetRates[index]);
            bucket.put("orderCount", counts[index]);
            bucket.put("actualRate", round(counts[index] * 100.0 / amounts.size()));
            bucket.put("gmv", gmvs[index].setScale(2, RoundingMode.HALF_UP));
            buckets.add(bucket);
        }
        response.put("sufficient", true);
        response.put("reason", null);
        response.put("buckets", buckets);
        return response;
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank() || "全部".equals(category.trim()) || "全部类型".equals(category.trim())) {
            return null;
        }
        String normalized = category.trim();
        if (!PROJECT_CATEGORIES.contains(normalized)) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "企划类型不存在");
        }
        return normalized;
    }

    private String projectSnapshotCategory(String projectSnapshot) {
        if (projectSnapshot == null || projectSnapshot.isBlank()) return null;
        try {
            JsonNode category = objectMapper.readTree(projectSnapshot).get("category");
            return category == null || category.isNull() ? null : blankAs(category.asText(), null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private BigDecimal percentileBoundary(List<BigDecimal> sorted, double percentile) {
        int cut = Math.max(1, Math.min(sorted.size() - 1, (int) Math.round(sorted.size() * percentile)));
        return sorted.get(cut - 1).add(sorted.get(cut)).divide(BigDecimal.valueOf(2));
    }

    private BigDecimal median(List<BigDecimal> sorted) {
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 1 ? sorted.get(middle)
                : sorted.get(middle - 1).add(sorted.get(middle)).divide(BigDecimal.valueOf(2));
    }

    private int bucketIndex(BigDecimal amount, List<BigDecimal> boundaries) {
        for (int index = 0; index < boundaries.size(); index++) {
            if (amount.compareTo(boundaries.get(index)) < 0) return index;
        }
        return boundaries.size();
    }

    private String currency(BigDecimal amount) {
        return "¥" + amount.stripTrailingZeros().toPlainString();
    }

    private List<Map<String, Object>> supplyDemandGap(Map<String, Long> demand, Map<String, Double> zeroRates) {
        List<Stall> stalls = stallMapper.selectList(new LambdaQueryWrapper<Stall>()
                .eq(Stall::getDeleted, 0).eq(Stall::getStatus, 1).gt(Stall::getStock, 0));
        Map<String, Long> activeStalls = countBy(stalls, stall -> blankAs(stall.getCategory(), "未分类"));
        Set<Long> certifiedPainters = userMapper.selectList(new LambdaQueryWrapper<User>()
                        .eq(User::getCertStatus, 2).eq(User::getStatus, 0).eq(User::getDeleted, 0))
                .stream().map(User::getId).collect(Collectors.toSet());
        List<Work> works = certifiedPainters.isEmpty() ? List.of() : workMapper.selectList(new LambdaQueryWrapper<Work>()
                .in(Work::getUserId, certifiedPainters).eq(Work::getVisibility, 1).eq(Work::getDeleted, 0));
        Map<String, Long> painters = new HashMap<>();
        works.stream().collect(Collectors.groupingBy(work -> blankAs(work.getCategory(), "未分类")))
                .forEach((category, categoryWorks) -> painters.put(category,
                        categoryWorks.stream().map(Work::getUserId).distinct().count()));
        Set<String> categories = new HashSet<>(demand.keySet());
        categories.addAll(activeStalls.keySet());
        return categories.stream().map(category -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", category);
                    item.put("demand", demand.getOrDefault(category, 0L));
                    item.put("activeStalls", activeStalls.getOrDefault(category, 0L));
                    item.put("certifiedPainters", painters.getOrDefault(category, 0L));
                    item.put("zeroApplicationRate", zeroRates.getOrDefault(category, 0.0));
                    return item;
                }).sorted(Comparator.comparingLong(item -> -(Long) item.get("demand"))).toList();
    }

    private List<Map<String, Object>> operationCategories(List<OrderInfo> completed) {
        Set<Long> projectIds = completed.stream().map(OrderInfo::getProjectId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Set<Long> stallIds = completed.stream().map(OrderInfo::getStallId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Project> projects = projectIds.isEmpty() ? Map.of() : projectMapper.selectBatchIds(projectIds)
                .stream().collect(Collectors.toMap(Project::getId, Function.identity()));
        Map<Long, Stall> stalls = stallIds.isEmpty() ? Map.of() : stallMapper.selectBatchIds(stallIds)
                .stream().collect(Collectors.toMap(Stall::getId, Function.identity()));
        Map<String, long[]> totals = new HashMap<>();
        Map<String, BigDecimal> amounts = new HashMap<>();
        for (OrderInfo order : completed) {
            String category = order.getProjectId() != null
                    ? blankAs(projects.get(order.getProjectId()) == null ? null : projects.get(order.getProjectId()).getCategory(), "未分类")
                    : blankAs(stalls.get(order.getStallId()) == null ? null : stalls.get(order.getStallId()).getCategory(), "未分类");
            totals.computeIfAbsent(category, ignored -> new long[1])[0]++;
            amounts.merge(category, order.getAmount() == null ? BigDecimal.ZERO : order.getAmount(), BigDecimal::add);
        }
        return totals.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0])).limit(5)
                .map(entry -> Map.<String, Object>of("name", entry.getKey(), "orders", entry.getValue()[0],
                        "gmv", amounts.getOrDefault(entry.getKey(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)))
                .toList();
    }

    private long countUsers(Range range) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getDeleted, 0)
                .ge(User::getCreatedAt, range.start()).lt(User::getCreatedAt, range.endExclusive()));
    }

    private long countOrders(Range range) {
        return orderInfoMapper.selectCount(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getDeleted, 0)
                .ge(OrderInfo::getCreatedAt, range.start()).lt(OrderInfo::getCreatedAt, range.endExclusive()));
    }

    private Map<String, Object> basicMetrics(Range range) {
        List<OrderInfo> completed = orderInfoMapper.selectList(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getDeleted, 0).eq(OrderInfo::getStatus, 3)
                .ge(OrderInfo::getCompletedAt, range.start()).lt(OrderInfo::getCompletedAt, range.endExclusive()));
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalUsers", userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getDeleted, 0)));
        metrics.put("newUsers", countUsers(range));
        metrics.put("newOrders", countOrders(range));
        metrics.put("completedOrders", (long) completed.size());
        metrics.put("gmv", money(completed));
        return metrics;
    }

    private Range resolve(LocalDate start, LocalDate end) {
        LocalDate actualEnd = end == null ? LocalDate.now() : end;
        LocalDate actualStart = start == null ? actualEnd.minusDays(6) : start;
        long days = ChronoUnit.DAYS.between(actualStart, actualEnd) + 1;
        if (days < 1 || days > 366) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "统计日期范围必须为1至366天");
        }
        return new Range(actualStart.atStartOfDay(), actualEnd.plusDays(1).atStartOfDay());
    }

    private Map<String, Object> rangeMap(Range range) {
        return Map.of("startDate", range.start().toLocalDate(), "endDate", range.endExclusive().minusDays(1).toLocalDate());
    }

    private String granularity(Range range) {
        long days = ChronoUnit.DAYS.between(range.start().toLocalDate(), range.endExclusive().toLocalDate());
        if (days <= 31) return "DAY";
        if (days <= 90) return "WEEK";
        return "MONTH";
    }

    private List<Map<String, Object>> timeline(Range range, Map<LocalDate, Long> primary,
                                               Map<LocalDate, Long> secondary) {
        String granularity = granularity(range);
        LocalDate first = range.start().toLocalDate();
        LocalDate last = range.endExclusive().minusDays(1).toLocalDate();
        List<Map<String, Object>> result = new ArrayList<>();
        if ("DAY".equals(granularity)) {
            for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1)) {
                result.add(timelineBucket(day, day, granularity, primary, secondary));
            }
            return result;
        }
        if ("WEEK".equals(granularity)) {
            for (LocalDate start = first; !start.isAfter(last); start = start.plusDays(7)) {
                LocalDate end = start.plusDays(6).isAfter(last) ? last : start.plusDays(6);
                result.add(timelineBucket(start, end, granularity, primary, secondary));
            }
            return result;
        }
        for (YearMonth month = YearMonth.from(first); !month.atDay(1).isAfter(last); month = month.plusMonths(1)) {
            LocalDate start = month.atDay(1).isBefore(first) ? first : month.atDay(1);
            LocalDate end = month.atEndOfMonth().isAfter(last) ? last : month.atEndOfMonth();
            result.add(timelineBucket(start, end, granularity, primary, secondary));
        }
        return result;
    }

    private Map<String, Object> timelineBucket(LocalDate start, LocalDate end, String granularity,
                                                Map<LocalDate, Long> primary,
                                                Map<LocalDate, Long> secondary) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("date", start);
        item.put("startDate", start);
        item.put("endDate", end);
        item.put("label", timelineLabel(start, end, granularity));
        item.put("value", sumRange(primary, start, end));
        if (secondary != null) item.put("secondary", sumRange(secondary, start, end));
        return item;
    }

    private long sumRange(Map<LocalDate, Long> values, LocalDate start, LocalDate end) {
        long total = 0;
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            total += values.getOrDefault(day, 0L);
        }
        return total;
    }

    private String timelineLabel(LocalDate start, LocalDate end, String granularity) {
        if ("MONTH".equals(granularity)) {
            return start.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        String startLabel = start.format(DateTimeFormatter.ofPattern("MM-dd"));
        if (start.equals(end)) return startLabel;
        return startLabel + "–" + end.format(DateTimeFormatter.ofPattern("MM-dd"));
    }

    private <T> Map<String, Long> countBy(Collection<T> values, Function<T, String> classifier) {
        return values.stream().collect(Collectors.groupingBy(classifier, LinkedHashMap::new, Collectors.counting()));
    }

    private List<Map<String, Object>> sorted(Map<String, Long> values, int limit) {
        return values.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(limit)
                .map(entry -> Map.<String, Object>of("name", entry.getKey(), "value", entry.getValue())).toList();
    }

    private BigDecimal money(List<OrderInfo> orders) {
        return orders.stream().map(OrderInfo::getAmount).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private String orderStatus(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待接单"; case 1 -> "进行中"; case 2 -> "待验收"; case 3 -> "已完成";
            case 4 -> "已取消"; case 5 -> "争议中"; default -> "未知";
        };
    }

    private String blankAs(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record Range(LocalDateTime start, LocalDateTime endExclusive) {}
}
