package com.artiv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.OrderDetailResponse;
import com.artiv.dto.OrderStageReviewRequest;
import com.artiv.dto.OrderStageSubmitRequest;
import com.artiv.dto.OrderReviewContext;
import com.artiv.entity.OrderEvent;
import com.artiv.entity.OrderInfo;
import com.artiv.entity.OrderStage;
import com.artiv.entity.Application;
import com.artiv.entity.Project;
import com.artiv.entity.Stall;
import com.artiv.entity.User;
import com.artiv.mapper.OrderEventMapper;
import com.artiv.mapper.OrderInfoMapper;
import com.artiv.mapper.OrderStageMapper;
import com.artiv.mapper.ApplicationMapper;
import com.artiv.mapper.ProjectMapper;
import com.artiv.mapper.StallMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.service.MessageService;
import com.artiv.service.OrderPreviewService;
import com.artiv.service.OrderService;
import com.artiv.service.AccountRestrictionService;
import com.artiv.service.ReviewService;
import com.artiv.service.UserBlockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.artiv.common.SystemMessageText.mark;
import static com.artiv.common.SystemMessageText.markTitle;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private static final int PENDING = 0;
    private static final int IN_PROGRESS = 1;
    private static final int PENDING_REVIEW = 2;
    private static final int COMPLETED = 3;
    private static final int CANCELLED = 4;
    private static final int AFTER_SALE = 5;
    private static final String DEFAULT_PHASES = "[{\"name\":\"初稿\"},{\"name\":\"修改\"},{\"name\":\"终稿\"}]";

    private final OrderInfoMapper orderInfoMapper;
    private final OrderStageMapper orderStageMapper;
    private final OrderEventMapper orderEventMapper;
    private final ApplicationMapper applicationMapper;
    private final ProjectMapper projectMapper;
    private final StallMapper stallMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;
    private final OrderPreviewService orderPreviewService;
    private final ObjectMapper objectMapper;
    private final AccountRestrictionService accountRestrictionService;
    private final ReviewService reviewService;
    private final UserBlockService userBlockService;

    @Override
    public Page<OrderInfo> listMine(long page, long size, Long userId, String source, String statusGroup,
                                    String participantRole) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getDeleted, 0);
        if ("employer".equalsIgnoreCase(participantRole)) {
            wrapper.eq(OrderInfo::getEmployerId, userId);
        } else if ("painter".equalsIgnoreCase(participantRole)) {
            wrapper.eq(OrderInfo::getPainterId, userId);
        } else {
            wrapper.and(w -> w.eq(OrderInfo::getEmployerId, userId).or().eq(OrderInfo::getPainterId, userId));
        }
        if ("stall".equalsIgnoreCase(source)) {
            wrapper.isNotNull(OrderInfo::getStallId);
        } else if ("project".equalsIgnoreCase(source)) {
            wrapper.isNotNull(OrderInfo::getProjectId);
        }
        if ("delivery".equalsIgnoreCase(statusGroup)) {
            wrapper.in(OrderInfo::getStatus, PENDING, IN_PROGRESS, PENDING_REVIEW);
        } else if ("review".equalsIgnoreCase(statusGroup)) {
            wrapper.eq(OrderInfo::getStatus, COMPLETED)
                    .ge(OrderInfo::getCompletedAt, LocalDateTime.now().minusDays(7))
                    .notExists("SELECT 1 FROM review r WHERE r.order_id = order_info.id AND r.from_id = " + userId);
        } else if ("afterSale".equalsIgnoreCase(statusGroup)) {
            wrapper.eq(OrderInfo::getStatus, AFTER_SALE);
        }
        wrapper.last("ORDER BY CASE "
                + "WHEN status IN (1, 2, 5) THEN 1 "
                + "WHEN status = 0 THEN 2 "
                + "ELSE 3 END ASC, created_at DESC, id DESC");
        return orderInfoMapper.selectPage(new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 50)), wrapper);
    }

    @Override
    @Transactional
    public OrderDetailResponse getDetail(Long id, Long userId) {
        OrderInfo order = requireParticipant(id, userId, false);
        OrderDetailResponse response = new OrderDetailResponse();
        response.setOrder(order);
        response.setViewerRole(userId.equals(order.getEmployerId()) ? "employer" : "painter");
        User employer = userMapper.selectById(order.getEmployerId());
        User painter = userMapper.selectById(order.getPainterId());
        response.setEmployerName(displayName(employer));
        response.setPainterName(displayName(painter));
        List<OrderStage> stages = orderStageMapper.selectList(new LambdaQueryWrapper<OrderStage>()
                .eq(OrderStage::getOrderId, id).eq(OrderStage::getDeleted, 0)
                .orderByAsc(OrderStage::getStageIndex).orderByAsc(OrderStage::getSubmissionNo));
        stages.forEach(this::repairLegacyPreview);
        if (userId.equals(order.getEmployerId()) && order.getStatus() != COMPLETED) {
            stages = stages.stream().map(this::withoutOriginals).toList();
        }
        response.setSubmissions(stages);
        response.setEvents(orderEventMapper.selectList(new LambdaQueryWrapper<OrderEvent>()
                .eq(OrderEvent::getOrderId, id).orderByAsc(OrderEvent::getCreatedAt)));
        OrderReviewContext reviewContext = reviewService.getOrderReviewContext(order, userId);
        if (reviewContext != null) {
            response.setReviewStatus(reviewContext.getStatus());
            response.setReviewDeadline(reviewContext.getDeadline());
            response.setViewerReview(reviewContext.getReview());
        }
        return response;
    }

    @Override
    @Transactional
    public OrderInfo createStallPurchase(Long stallId, Long employerId) {
        accountRestrictionService.requireAvailable(employerId, "购买橱窗服务");
        Stall stall = stallMapper.selectByIdForUpdate(stallId);
        if (stall == null || !Integer.valueOf(0).equals(stall.getDeleted())
                || !Integer.valueOf(1).equals(stall.getStatus())
                || stall.getStock() == null || stall.getStock() <= 0
                || employerId.equals(stall.getPainterId()) || stall.getPrice() == null
                || stall.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
        userBlockService.requireNotBlocked(employerId, stall.getPainterId(), "购买橱窗服务");
        User employer = userMapper.selectByIdForUpdate(employerId);
        User painter = userMapper.selectById(stall.getPainterId());
        if (!isActive(employer) || !isActive(painter)) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        BigDecimal balance = employer.getBalance() == null ? BigDecimal.ZERO : employer.getBalance();
        if (balance.compareTo(stall.getPrice()) < 0) {
            throw new BusinessException(ResultCode.BUDGET_NOT_ENOUGH);
        }
        employer.setBalance(balance.subtract(stall.getPrice()));
        employer.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(employer);
        stall.setStock(stall.getStock() - 1);
        stall.setUpdatedAt(LocalDateTime.now());
        stallMapper.updateById(stall);

        LocalDateTime now = LocalDateTime.now();
        OrderInfo order = new OrderInfo();
        order.setStallId(stallId);
        order.setEmployerId(employerId);
        order.setPainterId(stall.getPainterId());
        order.setAmount(stall.getPrice());
        order.setStallTitle(stall.getTitle());
        order.setStallCover(stall.getCover());
        order.setPhaseSnapshot(StringUtils.hasText(stall.getPhases()) ? stall.getPhases() : DEFAULT_PHASES);
        order.setStatus(PENDING);
        order.setCurrentStage(0);
        order.setDeleted(0);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        orderInfoMapper.insert(order);
        record(order, employerId, "CREATED", "企划方已下单，金额已托管并等待画师接单。", "ORDER_CREATED");
        messageService.createStallPurchaseCommunication(order, stall);
        return order;
    }

    @Override
    @Transactional
    public OrderInfo createProjectOrder(Long projectId, Long applicationId, Long employerId) {
        accountRestrictionService.requireAvailable(employerId, "发送企划订单");
        Application application = applicationMapper.selectByIdForUpdate(applicationId);
        if (application == null || !Integer.valueOf(0).equals(application.getDeleted())
                || !projectId.equals(application.getProjectId())
                || !Integer.valueOf(1).equals(application.getActive())) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null || !Integer.valueOf(0).equals(project.getDeleted())
                || !employerId.equals(project.getEmployerId())
                || !Integer.valueOf(0).equals(project.getStatus())
                || employerId.equals(application.getPainterId())) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
        userBlockService.requireNotBlocked(employerId, application.getPainterId(), "发送企划订单");
        long activeOrders = orderInfoMapper.selectCount(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getApplicationId, applicationId)
                .eq(OrderInfo::getDeleted, 0)
                .in(OrderInfo::getStatus, PENDING, IN_PROGRESS, PENDING_REVIEW, AFTER_SALE));
        if (activeOrders > 0) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "该画师已有一笔未完成的企划订单，请勿重复发送");
        }

        BigDecimal amount = application.getQuote() != null && application.getQuote().compareTo(BigDecimal.ZERO) > 0
                ? application.getQuote() : project.getBudget();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
        User employer = userMapper.selectByIdForUpdate(employerId);
        User painter = userMapper.selectById(application.getPainterId());
        if (!isActive(employer) || !isActive(painter)
                || !Integer.valueOf(2).equals(painter.getCertStatus())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        BigDecimal balance = employer.getBalance() == null ? BigDecimal.ZERO : employer.getBalance();
        if (balance.compareTo(amount) < 0) {
            throw new BusinessException(ResultCode.BUDGET_NOT_ENOUGH);
        }
        employer.setBalance(balance.subtract(amount));
        employer.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(employer);

        LocalDateTime now = LocalDateTime.now();
        OrderInfo order = new OrderInfo();
        order.setProjectId(projectId);
        order.setApplicationId(applicationId);
        order.setEmployerId(employerId);
        order.setPainterId(application.getPainterId());
        order.setAmount(amount);
        order.setProjectTitle(project.getTitle());
        order.setProjectCover(firstProjectImage(project.getImages()));
        order.setProjectSnapshot(projectSnapshot(project, application));
        order.setPhaseSnapshot(StringUtils.hasText(project.getPhases()) ? project.getPhases() : DEFAULT_PHASES);
        order.setStatus(PENDING);
        order.setCurrentStage(0);
        order.setDeleted(0);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        orderInfoMapper.insert(order);
        record(order, employerId, "CREATED", "企划方已发送企划订单，金额已托管并等待画师接单。", "ORDER_CREATED");
        messageService.createProjectOrderCommunication(order, project);
        return order;
    }

    @Override
    @Transactional
    public OrderInfo accept(Long id, Long userId) {
        OrderInfo order = requireParticipant(id, userId, true);
        if (!userId.equals(order.getPainterId()) || order.getStatus() != PENDING) flowError();
        User painter = userMapper.selectById(order.getPainterId());
        if (!isActive(painter) || !Integer.valueOf(2).equals(painter.getCertStatus())) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
        order.setStatus(IN_PROGRESS);
        order.setAcceptedAt(LocalDateTime.now());
        save(order);
        record(order, userId, "ACCEPTED",
                painterOrderLabel(painter, order) + "已确认接单，订单" + notificationOrderTitle(order)
                        + "已进入创作阶段。",
                "ORDER_ACCEPTED");
        return order;
    }

    @Override
    @Transactional
    public OrderInfo reject(Long id, Long userId, String reason) {
        OrderInfo order = requireParticipant(id, userId, true);
        if (!userId.equals(order.getPainterId()) || order.getStatus() != PENDING) flowError();
        return cancelAndRefund(order, userId, normalizeReason(reason, "画师拒绝接单"), "ORDER_REJECTED");
    }

    @Override
    @Transactional
    public OrderInfo cancel(Long id, Long userId, String reason) {
        OrderInfo order = requireParticipant(id, userId, true);
        if (!userId.equals(order.getEmployerId()) || order.getStatus() != PENDING) flowError();
        return cancelAndRefund(order, userId, normalizeReason(reason, "企划方取消订单"), "ORDER_CANCELLED");
    }

    @Override
    @Transactional
    public OrderInfo requestAfterSale(Long id, Long userId, String reason) {
        OrderInfo order = requireParticipant(id, userId, true);
        if (!userId.equals(order.getEmployerId())
                || (order.getStatus() != IN_PROGRESS && order.getStatus() != PENDING_REVIEW)) flowError();
        order.setStatus(AFTER_SALE);
        order.setAfterSaleReason(requireReason(reason));
        save(order);
        record(order, userId, "AFTER_SALE",
                "订单" + notificationOrderTitle(order) + "已申请退款/售后。原因：" + order.getAfterSaleReason(),
                "ORDER_AFTER_SALE");
        return order;
    }

    @Override
    @Transactional
    public OrderInfo submitStage(Long id, Long userId, OrderStageSubmitRequest request) {
        OrderInfo order = requireParticipant(id, userId, true);
        if (!userId.equals(order.getPainterId()) || order.getStatus() != IN_PROGRESS || request == null) flowError();
        String original = validateFile(request.getImage());
        String preview = orderPreviewService.createWatermarkedPreview(original);
        int stageIndex = order.getCurrentStage() == null ? 0 : order.getCurrentStage();
        List<Map<String, Object>> phases = phases(order);
        if (stageIndex < 0 || stageIndex >= phases.size()) flowError();
        long count = orderStageMapper.selectCount(new LambdaQueryWrapper<OrderStage>()
                .eq(OrderStage::getOrderId, id).eq(OrderStage::getStageIndex, stageIndex)
                .eq(OrderStage::getDeleted, 0));
        OrderStage stage = new OrderStage();
        stage.setOrderId(id);
        stage.setName(String.valueOf(phases.get(stageIndex).getOrDefault("name", "阶段 " + (stageIndex + 1))));
        stage.setStageIndex(stageIndex);
        stage.setSubmissionNo((int) count + 1);
        stage.setPercent(asPercent(phases.get(stageIndex).get("percent")));
        stage.setSubmitter(1);
        stage.setContent(trim(request.getContent(), 1000));
        stage.setAttachments(preview);
        stage.setOriginals(original);
        stage.setStatus(0);
        stage.setDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        stage.setCreatedAt(now);
        stage.setUpdatedAt(now);
        orderStageMapper.insert(stage);
        order.setStatus(PENDING_REVIEW);
        save(order);
        User painter = userMapper.selectById(order.getPainterId());
        record(order, userId, "STAGE_SUBMITTED",
                painterOrderLabel(painter, order) + "已提交订单" + notificationOrderTitle(order)
                        + "的" + mark(stage.getName()) + "，请验收。",
                "ORDER_STAGE_SUBMITTED");
        return order;
    }

    @Override
    @Transactional
    public OrderInfo reviewStage(Long id, Long userId, OrderStageReviewRequest request) {
        OrderInfo order = requireParticipant(id, userId, true);
        if (!userId.equals(order.getEmployerId()) || order.getStatus() != PENDING_REVIEW || request == null) flowError();
        OrderStage stage = latestCurrentSubmission(order);
        String action = request.getAction() == null ? "" : request.getAction().trim();
        stage.setReviewRemark(trim(request.getRemark(), 500));
        stage.setReviewedAt(LocalDateTime.now());
        if ("revise".equals(action)) {
            stage.setStatus(2);
            stage.setUpdatedAt(LocalDateTime.now());
            orderStageMapper.updateById(stage);
            order.setStatus(IN_PROGRESS);
            save(order);
            record(order, userId, "REVISION_REQUESTED",
                    "企划方要求修改订单" + notificationOrderTitle(order) + "的" + mark(stage.getName()) + "："
                            + normalizeReason(stage.getReviewRemark(), "请按沟通内容修改"),
                    "ORDER_REVISION_REQUESTED");
            return order;
        }
        if ("complete".equals(action)) {
            stage.setStatus(1);
            stage.setUpdatedAt(LocalDateTime.now());
            orderStageMapper.updateById(stage);
            settle(order);
            record(order, userId, "COMPLETED",
                    "企划方已确认订单" + notificationOrderTitle(order)
                            + "当前交付并直接完成交易，后续创作阶段已跳过，款项已结算。",
                    "ORDER_COMPLETED");
            return order;
        }
        if (!"approve".equals(action)) flowError();
        stage.setStatus(1);
        stage.setUpdatedAt(LocalDateTime.now());
        orderStageMapper.updateById(stage);
        int next = order.getCurrentStage() + 1;
        if (next < phases(order).size()) {
            order.setCurrentStage(next);
            order.setStatus(IN_PROGRESS);
            save(order);
            record(order, userId, "STAGE_APPROVED",
                    "企划方已通过订单" + notificationOrderTitle(order) + "的" + mark(stage.getName())
                            + "，已进入下一阶段。",
                    "ORDER_STAGE_APPROVED");
        } else {
            settle(order);
            record(order, userId, "COMPLETED",
                    "企划方已确认订单" + notificationOrderTitle(order) + "的最终交付，订单已完成并结算。",
                    "ORDER_COMPLETED");
        }
        return order;
    }

    @Override
    @Transactional
    public int cancelUnfinishedForBannedUser(Long userId, Long adminId, String reason) {
        List<Long> orderIds = orderInfoMapper.selectList(new LambdaQueryWrapper<OrderInfo>()
                        .and(w -> w.eq(OrderInfo::getEmployerId, userId).or().eq(OrderInfo::getPainterId, userId))
                        .in(OrderInfo::getStatus, PENDING, IN_PROGRESS, PENDING_REVIEW, AFTER_SALE)
                        .eq(OrderInfo::getDeleted, 0)
                        .orderByAsc(OrderInfo::getId))
                .stream().map(OrderInfo::getId).toList();
        int cancelled = 0;
        for (Long orderId : orderIds) {
            OrderInfo order = orderInfoMapper.selectByIdForUpdate(orderId);
            if (order != null && List.of(PENDING, IN_PROGRESS, PENDING_REVIEW, AFTER_SALE).contains(order.getStatus())) {
                cancelAndRefund(order, adminId, reason, "ADMIN_BAN_CANCEL");
                cancelled++;
            }
        }
        return cancelled;
    }

    @Override
    @Transactional
    public OrderInfo arbitrate(Long orderId, Long adminId, boolean settlePainter, String reason) {
        OrderInfo order = orderInfoMapper.selectByIdForUpdate(orderId);
        if (order == null || !Integer.valueOf(0).equals(order.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (!Integer.valueOf(AFTER_SALE).equals(order.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "订单不在待仲裁状态");
        }
        if (settlePainter) {
            settle(order);
            record(order, adminId, "ARBITRATION_SETTLED",
                    "订单" + notificationOrderTitle(order) + "已由管理员仲裁为全额结算给画师。说明："
                            + requireReason(reason),
                    "ORDER_ARBITRATION_SETTLED");
        } else {
            cancelAndRefund(order, adminId, requireReason(reason), "ORDER_ARBITRATION_REFUND");
        }
        return order;
    }

    private OrderInfo cancelAndRefund(OrderInfo order, Long actorId, String reason, String subtype) {
        User employer = userMapper.selectByIdForUpdate(order.getEmployerId());
        employer.setBalance((employer.getBalance() == null ? BigDecimal.ZERO : employer.getBalance()).add(order.getAmount()));
        employer.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(employer);
        if (order.getStallId() != null) {
            Stall stall = stallMapper.selectByIdForUpdate(order.getStallId());
            if (stall != null) {
                stall.setStock((stall.getStock() == null ? 0 : stall.getStock()) + 1);
                stall.setUpdatedAt(LocalDateTime.now());
                stallMapper.updateById(stall);
            }
        }
        order.setStatus(CANCELLED);
        order.setCancelReason(reason);
        order.setCancelledAt(LocalDateTime.now());
        save(order);
        String content;
        if ("ORDER_CANCELLED".equals(subtype)) {
            content = "订单" + notificationOrderTitle(order) + "已取消，托管金额已退回：" + reason;
        } else {
            String amount = order.getAmount() == null ? "0.00"
                    : order.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
            String action = switch (subtype) {
                case "ORDER_REJECTED" -> "画师已拒绝";
                case "ADMIN_BAN_CANCEL" -> "管理员因用户封禁已终止";
                case "ORDER_ARBITRATION_REFUND" -> "管理员仲裁已退款";
                default -> "企划方已取消";
            };
            content = order.getStallId() == null
                    ? action + "企划订单" + markTitle(displayOrderTitle(order.getProjectTitle(), order.getId()))
                        + "，托管金额 ¥" + amount + " 已退回。原因：" + reason
                    : action + "橱窗订单" + markTitle(displayOrderTitle(order.getStallTitle(), order.getId()))
                        + "，托管金额 ¥" + amount + " 已退回，库存已恢复 1 件。原因：" + reason;
        }
        record(order, actorId, "CANCELLED", content, subtype);
        return order;
    }

    private String displayOrderTitle(String title, Long orderId) {
        return StringUtils.hasText(title) ? title : "订单 #" + orderId;
    }

    private String notificationOrderTitle(OrderInfo order) {
        String title = StringUtils.hasText(order.getProjectTitle())
                ? order.getProjectTitle()
                : order.getStallTitle();
        return markTitle(displayOrderTitle(title, order.getId()));
    }

    private String painterOrderLabel(User painter, OrderInfo order) {
        if (painter != null && StringUtils.hasText(painter.getNickname())) {
            return "画师" + mark(painter.getNickname());
        }
        if (painter != null && StringUtils.hasText(painter.getUsername())) {
            return "画师" + mark(painter.getUsername());
        }
        return "画师" + mark("#" + order.getPainterId());
    }

    private String firstProjectImage(String images) {
        if (!StringUtils.hasText(images)) return null;
        String value = images.trim();
        try {
            List<String> parsed = objectMapper.readValue(value, new TypeReference<>() {});
            return parsed == null || parsed.isEmpty() ? null : trim(parsed.get(0), 500);
        } catch (Exception ignored) {
            return trim(value.split(",")[0], 500);
        }
    }

    private String projectSnapshot(Project project, Application application) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("title", project.getTitle());
        snapshot.put("description", project.getDescription());
        snapshot.put("category", project.getCategory());
        snapshot.put("subject", project.getSubject());
        snapshot.put("requirements", project.getRequirements());
        snapshot.put("phases", project.getPhases());
        // 快照字段使用稳定的 ISO 日期字符串，避免 ObjectMapper 日期配置差异改写历史格式。
        snapshot.put("deadline", project.getDeadline() == null ? null : project.getDeadline().toString());
        snapshot.put("applicationId", application.getId());
        snapshot.put("applicationIntro", application.getIntro());
        snapshot.put("applicationQuote", application.getQuote());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception exception) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
    }

    private void settle(OrderInfo order) {
        User painter = userMapper.selectByIdForUpdate(order.getPainterId());
        if (painter == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        painter.setBalance((painter.getBalance() == null ? BigDecimal.ZERO : painter.getBalance()).add(order.getAmount()));
        if (userMapper.incrementBalance(painter.getId(), order.getAmount()) != 1) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        order.setStatus(COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        save(order);
        if (order.getProjectId() != null && order.getApplicationId() != null) {
            Application application = applicationMapper.selectByIdForUpdate(order.getApplicationId());
            if (application != null && Integer.valueOf(0).equals(application.getDeleted())
                    && Integer.valueOf(1).equals(application.getActive())) {
                LocalDateTime now = LocalDateTime.now();
                application.setStatus(3);
                application.setActive(0);
                application.setEndedReason("ORDER_COMPLETED");
                application.setEndedAt(now);
                application.setUpdatedAt(now);
                applicationMapper.updateById(application);
            }
        }
    }

    private OrderInfo requireParticipant(Long id, Long userId, boolean lock) {
        OrderInfo order = lock ? orderInfoMapper.selectByIdForUpdate(id) : orderInfoMapper.selectById(id);
        if (order == null || !Integer.valueOf(0).equals(order.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (!userId.equals(order.getEmployerId()) && !userId.equals(order.getPainterId())) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
        return order;
    }

    private OrderStage latestCurrentSubmission(OrderInfo order) {
        List<OrderStage> stages = orderStageMapper.selectList(new LambdaQueryWrapper<OrderStage>()
                .eq(OrderStage::getOrderId, order.getId())
                .eq(OrderStage::getStageIndex, order.getCurrentStage())
                .eq(OrderStage::getStatus, 0).eq(OrderStage::getDeleted, 0)
                .orderByDesc(OrderStage::getSubmissionNo).last("LIMIT 1"));
        if (stages.isEmpty()) flowError();
        return stages.get(0);
    }

    private void record(OrderInfo order, Long actorId, String type, String content, String messageSubtype) {
        OrderEvent event = new OrderEvent();
        event.setOrderId(order.getId());
        event.setActorId(actorId);
        event.setEventType(type);
        event.setContent(content);
        event.setCreatedAt(LocalDateTime.now());
        orderEventMapper.insert(event);
        if (!"CREATED".equals(type)) {
            messageService.createOrderEventMessage(order, actorId, messageSubtype, content);
        }
    }

    private void save(OrderInfo order) {
        order.setUpdatedAt(LocalDateTime.now());
        orderInfoMapper.updateById(order);
    }

    private List<Map<String, Object>> phases(OrderInfo order) {
        try {
            List<Map<String, Object>> result = objectMapper.readValue(
                    StringUtils.hasText(order.getPhaseSnapshot()) ? order.getPhaseSnapshot() : DEFAULT_PHASES,
                    new TypeReference<>() {});
            if (result == null || result.isEmpty()) throw new IllegalArgumentException();
            return result;
        } catch (Exception exception) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
    }

    private String validateFile(String value) {
        String result = trim(value, 500);
        if (!StringUtils.hasText(result) || !result.startsWith("/uploads/")) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
        return result;
    }

    private OrderStage withoutOriginals(OrderStage source) {
        OrderStage copy = new OrderStage();
        BeanUtils.copyProperties(source, copy);
        copy.setOriginals(null);
        return copy;
    }

    private void repairLegacyPreview(OrderStage stage) {
        if (!StringUtils.hasText(stage.getOriginals())
                || orderPreviewService.isGeneratedPreview(stage.getAttachments())) {
            return;
        }
        try {
            String preview = orderPreviewService.ensureWatermarkedPreview(stage.getAttachments(), stage.getOriginals());
            if (!preview.equals(stage.getAttachments())) {
                stage.setAttachments(preview);
                stage.setUpdatedAt(LocalDateTime.now());
                orderStageMapper.updateById(stage);
            }
        } catch (BusinessException exception) {
            // 历史原图若已被人工清理，订单详情仍应可打开；保留旧附件并记录待维护信息。
            log.warn("无法补生成订单阶段预览图，stageId={}, original={}", stage.getId(), stage.getOriginals());
        }
    }

    private String displayName(User user) {
        if (user == null) return "未知用户";
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    private boolean isActive(User user) {
        return user != null && Integer.valueOf(0).equals(user.getDeleted()) && Integer.valueOf(0).equals(user.getStatus());
    }

    private String requireReason(String reason) {
        String value = trim(reason, 500);
        if (!StringUtils.hasText(value)) throw new BusinessException(ResultCode.PARAM_INVALID);
        return value;
    }

    private String normalizeReason(String reason, String fallback) {
        String value = trim(reason, 500);
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String trim(String value, int max) {
        if (value == null) return null;
        String result = value.trim();
        return result.length() > max ? result.substring(0, max) : result;
    }

    private int asPercent(Object value) {
        try { return Math.max(0, Math.min(100, Integer.parseInt(String.valueOf(value)))); }
        catch (Exception ignored) { return 0; }
    }

    private void flowError() {
        throw new BusinessException(ResultCode.STATUS_FLOW_INVALID);
    }
}
