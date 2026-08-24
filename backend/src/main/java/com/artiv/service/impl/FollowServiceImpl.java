package com.artiv.service.impl;

import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.FollowStateResponse;
import com.artiv.dto.FollowedPainterItem;
import com.artiv.entity.User;
import com.artiv.entity.UserFollow;
import com.artiv.mapper.UserFollowMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.service.AccountRestrictionService;
import com.artiv.service.FollowService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final UserFollowMapper userFollowMapper;
    private final UserMapper userMapper;
    private final AccountRestrictionService accountRestrictionService;

    @Override
    @Transactional
    public FollowStateResponse followPainter(Long painterId, Long userId) {
        accountRestrictionService.requireAvailable(userId, "关注画师");
        requireFollowablePainter(painterId);
        if (painterId.equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "不能关注自己");
        }
        if (isFollowing(painterId, userId)) {
            return new FollowStateResponse(painterId, true);
        }
        UserFollow relation = new UserFollow();
        relation.setUserId(userId);
        relation.setPainterId(painterId);
        relation.setCreatedAt(LocalDateTime.now());
        try {
            userFollowMapper.insert(relation);
        } catch (DuplicateKeyException ignored) {
            // 联合唯一索引处理并发重复点击，接口保持幂等。
        }
        return new FollowStateResponse(painterId, true);
    }

    @Override
    @Transactional
    public FollowStateResponse unfollowPainter(Long painterId, Long userId) {
        userFollowMapper.delete(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getUserId, userId)
                .eq(UserFollow::getPainterId, painterId));
        return new FollowStateResponse(painterId, false);
    }

    @Override
    public FollowStateResponse getPainterState(Long painterId, Long userId) {
        return new FollowStateResponse(painterId, isFollowing(painterId, userId));
    }

    @Override
    public Page<FollowedPainterItem> listPainters(long page, long size, Long userId) {
        return userFollowMapper.selectMine(new Page<>(safePage(page), safeSize(size)), userId);
    }

    private boolean isFollowing(Long painterId, Long userId) {
        Long count = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getUserId, userId)
                .eq(UserFollow::getPainterId, painterId));
        return count != null && count > 0;
    }

    private void requireFollowablePainter(Long painterId) {
        User painter = userMapper.selectById(painterId);
        if (painter == null || Integer.valueOf(1).equals(painter.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (!Integer.valueOf(0).equals(painter.getStatus())
                || !Integer.valueOf(2).equals(painter.getCertStatus())) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "只能关注正常状态的已认证画师");
        }
    }

    private long safePage(long page) {
        return Math.max(1, page);
    }

    private long safeSize(long size) {
        return Math.min(50, Math.max(1, size));
    }
}
