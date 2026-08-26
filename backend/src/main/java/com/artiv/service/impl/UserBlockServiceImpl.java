package com.artiv.service.impl;

import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.BlockStatusResponse;
import com.artiv.dto.BlockedUserItem;
import com.artiv.entity.OrderInfo;
import com.artiv.entity.User;
import com.artiv.entity.UserBlock;
import com.artiv.mapper.ApplicationMapper;
import com.artiv.mapper.InvitationMapper;
import com.artiv.mapper.OrderInfoMapper;
import com.artiv.mapper.UserBlockMapper;
import com.artiv.mapper.UserFollowMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.service.AccountRestrictionService;
import com.artiv.service.UserBlockService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserBlockServiceImpl implements UserBlockService {
    private static final List<Integer> NON_TERMINAL_ORDER_STATUSES = List.of(0, 1, 2, 5);

    private final UserBlockMapper userBlockMapper;
    private final UserMapper userMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final UserFollowMapper userFollowMapper;
    private final InvitationMapper invitationMapper;
    private final ApplicationMapper applicationMapper;
    private final AccountRestrictionService accountRestrictionService;

    @Override
    public Page<BlockedUserItem> listMine(long page, long size, Long blockerId) {
        return userBlockMapper.selectBlockedUsers(
                new Page<>(Math.max(1, page), Math.min(50, Math.max(1, size))), blockerId);
    }

    @Override
    public BlockStatusResponse getStatus(Long blockerId, Long targetId) {
        requireIds(blockerId, targetId);
        return new BlockStatusResponse(targetId, isBlockedBy(blockerId, targetId));
    }

    @Override
    @Transactional
    public BlockStatusResponse block(Long blockerId, Long targetId) {
        requireIds(blockerId, targetId);
        accountRestrictionService.requireAvailable(blockerId, "加入黑名单");
        if (blockerId.equals(targetId)) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "不能将自己加入黑名单");
        }
        if (isBlockedBy(blockerId, targetId)) {
            return new BlockStatusResponse(targetId, true);
        }
        User target = userMapper.selectById(targetId);
        if (target == null || Integer.valueOf(1).equals(target.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        long activeOrders = orderInfoMapper.selectCount(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getDeleted, 0)
                .in(OrderInfo::getStatus, NON_TERMINAL_ORDER_STATUSES)
                .and(wrapper -> wrapper
                        .eq(OrderInfo::getEmployerId, blockerId).eq(OrderInfo::getPainterId, targetId)
                        .or(inner -> inner.eq(OrderInfo::getEmployerId, targetId)
                                .eq(OrderInfo::getPainterId, blockerId))));
        if (activeOrders > 0) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(),
                    "您与该用户仍有未结束的订单，请先完成交易或售后处理后再加入黑名单");
        }

        LocalDateTime now = LocalDateTime.now();
        UserBlock relation = new UserBlock();
        relation.setBlockerId(blockerId);
        relation.setBlockedId(targetId);
        relation.setCreatedAt(now);
        try {
            userBlockMapper.insert(relation);
        } catch (DuplicateKeyException ignored) {
            return new BlockStatusResponse(targetId, true);
        }
        userFollowMapper.deleteBetweenUsers(blockerId, targetId);
        invitationMapper.invalidateBetweenUsers(blockerId, targetId, now);
        applicationMapper.endBetweenUsers(blockerId, targetId, now);
        return new BlockStatusResponse(targetId, true);
    }

    @Override
    @Transactional
    public BlockStatusResponse unblock(Long blockerId, Long targetId) {
        requireIds(blockerId, targetId);
        userBlockMapper.delete(new LambdaQueryWrapper<UserBlock>()
                .eq(UserBlock::getBlockerId, blockerId)
                .eq(UserBlock::getBlockedId, targetId));
        return new BlockStatusResponse(targetId, false);
    }

    @Override
    public boolean isEitherBlocked(Long firstUserId, Long secondUserId) {
        if (firstUserId == null || secondUserId == null || firstUserId.equals(secondUserId)) return false;
        Long count = userBlockMapper.selectCount(new LambdaQueryWrapper<UserBlock>()
                .and(wrapper -> wrapper
                        .eq(UserBlock::getBlockerId, firstUserId).eq(UserBlock::getBlockedId, secondUserId)
                        .or(inner -> inner.eq(UserBlock::getBlockerId, secondUserId)
                                .eq(UserBlock::getBlockedId, firstUserId))));
        return count != null && count > 0;
    }

    @Override
    public void requireNotBlocked(Long firstUserId, Long secondUserId, String action) {
        if (isEitherBlocked(firstUserId, secondUserId)) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(),
                    "当前无法与该用户" + (action == null ? "互动" : action));
        }
    }

    private boolean isBlockedBy(Long blockerId, Long targetId) {
        Long count = userBlockMapper.selectCount(new LambdaQueryWrapper<UserBlock>()
                .eq(UserBlock::getBlockerId, blockerId)
                .eq(UserBlock::getBlockedId, targetId));
        return count != null && count > 0;
    }

    private void requireIds(Long first, Long second) {
        if (first == null || second == null) throw new BusinessException(ResultCode.PARAM_INVALID);
    }
}
