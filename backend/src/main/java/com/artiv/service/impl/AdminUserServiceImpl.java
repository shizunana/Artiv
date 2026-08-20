package com.artiv.service.impl;

import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.admin.AdminUserStatusRequest;
import com.artiv.entity.ArtistCertification;
import com.artiv.entity.OrderInfo;
import com.artiv.entity.OrderDispute;
import com.artiv.entity.User;
import com.artiv.mapper.ArtistCertificationMapper;
import com.artiv.mapper.OrderInfoMapper;
import com.artiv.mapper.OrderDisputeMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.service.AdminAuditService;
import com.artiv.service.AdminUserService;
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

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {
    private final UserMapper userMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final ArtistCertificationMapper certificationMapper;
    private final OrderDisputeMapper disputeMapper;
    private final OrderService orderService;
    private final MessageService messageService;
    private final AdminAuditService auditService;

    @Override
    public Map<String, Object> list(long page, long size, Integer role, Integer status, Integer certStatus, String keyword) {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>()
                .eq(role != null, User::getRole, role)
                .eq(status != null, User::getStatus, status)
                .eq(certStatus != null, User::getCertStatus, certStatus)
                .and(keyword != null && !keyword.isBlank(), w -> w.like(User::getUsername, keyword == null ? null : keyword.trim())
                        .or().like(User::getNickname, keyword == null ? null : keyword.trim()))
                .orderByDesc(User::getCreatedAt);
        Page<User> result = userMapper.selectPage(new Page<>(page, Math.min(size, 100)), query);
        result.getRecords().forEach(user -> user.setPassword(null));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("records", result.getRecords());
        response.put("total", result.getTotal());
        response.put("current", result.getCurrent());
        response.put("size", result.getSize());
        return response;
    }

    @Override
    public Map<String, Object> detail(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        user.setPassword(null);
        List<OrderInfo> orders = orderInfoMapper.selectList(new LambdaQueryWrapper<OrderInfo>()
                .and(w -> w.eq(OrderInfo::getEmployerId, id).or().eq(OrderInfo::getPainterId, id))
                .orderByDesc(OrderInfo::getCreatedAt)
                .last("LIMIT 20"));
        List<ArtistCertification> certifications = certificationMapper.selectList(
                new LambdaQueryWrapper<ArtistCertification>()
                        .eq(ArtistCertification::getUserId, id)
                        .orderByDesc(ArtistCertification::getCreatedAt));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("user", user);
        response.put("recentOrders", orders);
        response.put("certifications", certifications);
        return response;
    }

    @Override
    @Transactional
    public Map<String, Object> changeStatus(Long adminId, Long id, AdminUserStatusRequest request) {
        if (adminId.equals(id)) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "不能修改自己的封禁状态");
        }
        if (request.getStatus() == null || (request.getStatus() != 0 && request.getStatus() != 1)) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
        User user = userMapper.selectByIdForUpdate(id);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (Integer.valueOf(3).equals(user.getRole())) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "管理员账号不能通过用户管理封禁");
        }

        User before = banSnapshot(user);
        int cancelledOrders = 0;
        if (request.getStatus() == 1) {
            if (request.getBannedUntil() != null && !request.getBannedUntil().isAfter(LocalDateTime.now())) {
                throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "封禁截止时间必须晚于当前时间");
            }
            String reason = request.getReason().trim();
            cancelledOrders = orderService.cancelUnfinishedForBannedUser(id, adminId,
                    "管理员封禁用户：" + reason);
            resolveRefundedDisputes(id, adminId, reason);
            user.setStatus(1);
            user.setBanReason(reason);
            user.setBannedBy(adminId);
            user.setBannedAt(LocalDateTime.now());
            user.setBannedUntil(request.getBannedUntil());
            messageService.createNotification(id, 0, "ACCOUNT_BANNED", null,
                    "账号已被封禁，期间不能发布需求、应征、创建橱窗或发起新订单。原因：" + reason);
        } else {
            user.setStatus(0);
            user.setBanReason(null);
            user.setBannedBy(null);
            user.setBannedAt(null);
            user.setBannedUntil(null);
            messageService.createNotification(id, 0, "ACCOUNT_UNBANNED", null,
                    "您的账号封禁已解除，可以继续使用交易功能。说明：" + request.getReason().trim());
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        auditService.record(adminId, request.getStatus() == 1 ? "BAN_USER" : "UNBAN_USER",
                "USER", id, before, banSnapshot(user), request.getReason().trim());

        Map<String, Object> response = new LinkedHashMap<>();
        user.setPassword(null);
        response.put("user", user);
        response.put("cancelledOrders", cancelledOrders);
        return response;
    }

    private User banSnapshot(User user) {
        User snapshot = new User();
        snapshot.setId(user.getId());
        snapshot.setStatus(user.getStatus());
        snapshot.setBanReason(user.getBanReason());
        snapshot.setBannedBy(user.getBannedBy());
        snapshot.setBannedAt(user.getBannedAt());
        snapshot.setBannedUntil(user.getBannedUntil());
        return snapshot;
    }

    private void resolveRefundedDisputes(Long userId, Long adminId, String reason) {
        List<OrderDispute> pending = disputeMapper.selectList(new LambdaQueryWrapper<OrderDispute>()
                .eq(OrderDispute::getStatus, 0));
        for (OrderDispute dispute : pending) {
            OrderInfo order = orderInfoMapper.selectById(dispute.getOrderId());
            if (order == null || !Integer.valueOf(4).equals(order.getStatus())
                    || (!userId.equals(order.getEmployerId()) && !userId.equals(order.getPainterId()))) {
                continue;
            }
            OrderDispute before = new OrderDispute();
            before.setId(dispute.getId());
            before.setStatus(dispute.getStatus());
            dispute.setStatus(1);
            dispute.setResult(1);
            dispute.setResolutionReason("用户封禁触发订单全额退款：" + reason);
            dispute.setResolvedBy(adminId);
            dispute.setResolvedAt(LocalDateTime.now());
            dispute.setUpdatedAt(LocalDateTime.now());
            disputeMapper.updateById(dispute);
            auditService.record(adminId, "RESOLVE_DISPUTE_ON_BAN", "ORDER_DISPUTE",
                    dispute.getId(), before, dispute, reason);
        }
    }
}
