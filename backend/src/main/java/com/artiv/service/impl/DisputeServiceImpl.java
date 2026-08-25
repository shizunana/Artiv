package com.artiv.service.impl;

import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.admin.AdminArbitrationRequest;
import com.artiv.dto.admin.DisputeCreateRequest;
import com.artiv.dto.admin.DisputeEvidenceRequest;
import com.artiv.entity.OrderDispute;
import com.artiv.entity.OrderDisputeEvidence;
import com.artiv.entity.OrderEvent;
import com.artiv.entity.OrderInfo;
import com.artiv.entity.User;
import com.artiv.mapper.OrderDisputeEvidenceMapper;
import com.artiv.mapper.OrderDisputeMapper;
import com.artiv.mapper.OrderEventMapper;
import com.artiv.mapper.OrderInfoMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.service.AdminAuditService;
import com.artiv.service.DisputeService;
import com.artiv.service.MessageService;
import com.artiv.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.artiv.common.SystemMessageText.markTitle;

@Service
@RequiredArgsConstructor
public class DisputeServiceImpl implements DisputeService {
    private static final Set<Integer> DISPUTABLE_STATUSES = Set.of(1, 2);

    private final OrderDisputeMapper disputeMapper;
    private final OrderDisputeEvidenceMapper evidenceMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderEventMapper orderEventMapper;
    private final UserMapper userMapper;
    private final OrderService orderService;
    private final MessageService messageService;
    private final AdminAuditService auditService;

    @Override
    @Transactional
    public OrderDispute create(Long orderId, Long userId, DisputeCreateRequest request) {
        if (request == null || request.getReason() == null || request.getReason().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "争议原因不能为空");
        }
        OrderInfo order = orderInfoMapper.selectByIdForUpdate(orderId);
        requireParticipant(order, userId);
        if (!DISPUTABLE_STATUSES.contains(order.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "仅进行中或待验收订单可发起争议");
        }
        if (disputeMapper.selectCount(new LambdaQueryWrapper<OrderDispute>()
                .eq(OrderDispute::getOrderId, orderId)) > 0) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "该订单已经存在争议记录");
        }
        validateImages(request.getImages(), false);

        LocalDateTime now = LocalDateTime.now();
        OrderDispute dispute = new OrderDispute();
        dispute.setOrderId(orderId);
        dispute.setInitiatorId(userId);
        dispute.setReason(request.getReason().trim());
        dispute.setStatus(0);
        dispute.setDeleted(0);
        dispute.setCreatedAt(now);
        dispute.setUpdatedAt(now);
        disputeMapper.insert(dispute);
        insertEvidence(dispute.getId(), userId, request.getImages(), request.getDescription());

        order.setStatus(5);
        order.setAfterSaleReason(dispute.getReason());
        order.setUpdatedAt(now);
        orderInfoMapper.updateById(order);
        record(order, userId, "DISPUTE_CREATED",
                "订单" + notificationOrderTitle(order) + "已发起争议并等待管理员仲裁。原因：" + dispute.getReason(),
                "ORDER_DISPUTE_CREATED");
        return dispute;
    }

    @Override
    public Map<String, Object> current(Long orderId, Long userId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        requireParticipant(order, userId);
        OrderDispute dispute = disputeMapper.selectOne(new LambdaQueryWrapper<OrderDispute>()
                .eq(OrderDispute::getOrderId, orderId).last("LIMIT 1"));
        if (dispute == null) {
            return Map.of();
        }
        return detail(dispute, order);
    }

    @Override
    @Transactional
    public Map<String, Object> addEvidence(Long disputeId, Long userId, DisputeEvidenceRequest request) {
        OrderDispute dispute = disputeMapper.selectByIdForUpdate(disputeId);
        if (dispute == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        OrderInfo order = orderInfoMapper.selectById(dispute.getOrderId());
        requireParticipant(order, userId);
        if (!Integer.valueOf(0).equals(dispute.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "争议已处理，不能补充证据");
        }
        if (evidenceMapper.selectCount(new LambdaQueryWrapper<OrderDisputeEvidence>()
                .eq(OrderDisputeEvidence::getDisputeId, disputeId)
                .eq(OrderDisputeEvidence::getSubmittedBy, userId)) > 0) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "每一方只能提交一次图片证据");
        }
        validateImages(request.getImages(), true);
        insertEvidence(disputeId, userId, request.getImages(), request.getDescription());
        record(order, userId, "DISPUTE_EVIDENCE_ADDED",
                "订单" + notificationOrderTitle(order) + "的争议已补充图片证据。", "ORDER_DISPUTE_EVIDENCE");
        return detail(dispute, order);
    }

    @Override
    public Map<String, Object> listAdmin(long page, long size, Integer status, String keyword) {
        Set<Long> orderIds = null;
        if (keyword != null && !keyword.isBlank()) {
            String value = keyword.trim();
            orderIds = orderInfoMapper.selectList(new LambdaQueryWrapper<OrderInfo>()
                            .like(OrderInfo::getProjectTitle, value).or().like(OrderInfo::getStallTitle, value))
                    .stream().map(OrderInfo::getId).collect(Collectors.toSet());
            try {
                orderIds.add(Long.valueOf(value));
            } catch (NumberFormatException ignored) {
                // 关键字不是订单号时只按标题查询。
            }
            if (orderIds.isEmpty()) {
                return Map.of("records", List.of(), "total", 0L, "current", page, "size", size);
            }
        }
        Page<OrderDispute> result = disputeMapper.selectPage(new Page<>(page, Math.min(size, 100)),
                new LambdaQueryWrapper<OrderDispute>()
                        .eq(status != null, OrderDispute::getStatus, status)
                        .in(orderIds != null, OrderDispute::getOrderId, orderIds)
                        .orderByAsc(OrderDispute::getStatus)
                        .orderByDesc(OrderDispute::getCreatedAt));
        List<Map<String, Object>> records = new ArrayList<>();
        for (OrderDispute dispute : result.getRecords()) {
            OrderInfo order = orderInfoMapper.selectById(dispute.getOrderId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("dispute", dispute);
            item.put("order", order);
            item.put("initiator", safeUser(dispute.getInitiatorId()));
            records.add(item);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("records", records);
        response.put("total", result.getTotal());
        response.put("current", result.getCurrent());
        response.put("size", result.getSize());
        return response;
    }

    @Override
    public Map<String, Object> detailAdmin(Long id) {
        OrderDispute dispute = disputeMapper.selectById(id);
        if (dispute == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        return detail(dispute, orderInfoMapper.selectById(dispute.getOrderId()));
    }

    @Override
    @Transactional
    public Map<String, Object> arbitrate(Long adminId, Long id, AdminArbitrationRequest request) {
        String result = request.getResult().trim().toUpperCase();
        if ("PARTIAL_REFUND".equals(result)) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "部分退款能力仅预留，本期不启用");
        }
        if (!"REFUND_EMPLOYER".equals(result) && !"SETTLE_PAINTER".equals(result)) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "仲裁结果不受支持");
        }
        OrderDispute dispute = disputeMapper.selectByIdForUpdate(id);
        if (dispute == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (!Integer.valueOf(0).equals(dispute.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "该争议已经处理");
        }
        OrderDispute before = new OrderDispute();
        before.setId(dispute.getId());
        before.setStatus(dispute.getStatus());
        orderService.arbitrate(dispute.getOrderId(), adminId, "SETTLE_PAINTER".equals(result), request.getReason());

        dispute.setStatus(1);
        dispute.setResult("SETTLE_PAINTER".equals(result) ? 2 : 1);
        dispute.setResolutionReason(request.getReason().trim());
        dispute.setResolvedBy(adminId);
        dispute.setResolvedAt(LocalDateTime.now());
        dispute.setUpdatedAt(LocalDateTime.now());
        disputeMapper.updateById(dispute);
        auditService.record(adminId, "ARBITRATE_DISPUTE", "ORDER_DISPUTE", dispute.getId(), before, dispute,
                request.getReason().trim());
        return detail(dispute, orderInfoMapper.selectById(dispute.getOrderId()));
    }

    private Map<String, Object> detail(OrderDispute dispute, OrderInfo order) {
        if (order == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("dispute", dispute);
        response.put("order", order);
        response.put("employer", safeUser(order.getEmployerId()));
        response.put("painter", safeUser(order.getPainterId()));
        response.put("evidence", evidenceMapper.selectList(new LambdaQueryWrapper<OrderDisputeEvidence>()
                .eq(OrderDisputeEvidence::getDisputeId, dispute.getId())
                .orderByAsc(OrderDisputeEvidence::getCreatedAt)));
        response.put("events", orderEventMapper.selectList(new LambdaQueryWrapper<OrderEvent>()
                .eq(OrderEvent::getOrderId, order.getId())
                .orderByAsc(OrderEvent::getCreatedAt)));
        return response;
    }

    private void insertEvidence(Long disputeId, Long userId, List<String> images, String description) {
        if (images == null) {
            return;
        }
        for (String image : images) {
            OrderDisputeEvidence evidence = new OrderDisputeEvidence();
            evidence.setDisputeId(disputeId);
            evidence.setSubmittedBy(userId);
            evidence.setImage(image.trim());
            evidence.setDescription(description == null ? null : description.trim());
            evidence.setCreatedAt(LocalDateTime.now());
            evidenceMapper.insert(evidence);
        }
    }

    private void validateImages(List<String> images, boolean required) {
        if (required && (images == null || images.isEmpty())) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "请至少上传一张证据图片");
        }
        if (images == null) {
            return;
        }
        if (images.size() > 9 || images.stream().anyMatch(image -> image == null
                || !image.startsWith("/uploads/") || image.contains(".."))) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "证据图片不合法或超过9张");
        }
    }

    private void requireParticipant(OrderInfo order, Long userId) {
        if (order == null || Integer.valueOf(1).equals(order.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (!userId.equals(order.getEmployerId()) && !userId.equals(order.getPainterId())) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
    }

    private User safeUser(Long id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    private void record(OrderInfo order, Long actorId, String eventType, String content, String subtype) {
        OrderEvent event = new OrderEvent();
        event.setOrderId(order.getId());
        event.setActorId(actorId);
        event.setEventType(eventType);
        event.setContent(content);
        event.setCreatedAt(LocalDateTime.now());
        orderEventMapper.insert(event);
        messageService.createOrderEventMessage(order, actorId, subtype, content);
    }

    private String notificationOrderTitle(OrderInfo order) {
        String title = order.getProjectId() != null ? order.getProjectTitle() : order.getStallTitle();
        if (title == null || title.isBlank()) {
            title = "订单 #" + order.getId();
        }
        return markTitle(title);
    }
}
