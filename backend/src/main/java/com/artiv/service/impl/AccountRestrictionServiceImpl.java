package com.artiv.service.impl;

import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.entity.User;
import com.artiv.mapper.UserMapper;
import com.artiv.service.AccountRestrictionService;
import com.artiv.service.AdminAuditService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountRestrictionServiceImpl implements AccountRestrictionService {
    private final UserMapper userMapper;
    private final AdminAuditService auditService;

    @Override
    @Transactional
    public User refresh(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (isExpired(user, LocalDateTime.now())) {
            User before = copyBanState(user);
            clearBan(user);
            userMapper.updateById(user);
            auditService.record(null, "AUTO_UNBAN_USER", "USER", user.getId(), before, user, "限时封禁到期自动解封");
        }
        return user;
    }

    @Override
    public void requireAvailable(Long userId, String actionName) {
        User user = refresh(userId);
        if (Integer.valueOf(1).equals(user.getStatus())) {
            String until = user.getBannedUntil() == null ? "永久" : "至 " + user.getBannedUntil();
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(),
                    "账号封禁期间不能" + actionName + "（" + until + "）");
        }
    }

    @Override
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public int releaseExpiredBans() {
        LocalDateTime now = LocalDateTime.now();
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .isNotNull(User::getBannedUntil)
                .le(User::getBannedUntil, now));
        for (User user : users) {
            User before = copyBanState(user);
            clearBan(user);
            userMapper.updateById(user);
            auditService.record(null, "AUTO_UNBAN_USER", "USER", user.getId(), before, user, "限时封禁到期自动解封");
        }
        return users.size();
    }

    private boolean isExpired(User user, LocalDateTime now) {
        return Integer.valueOf(1).equals(user.getStatus())
                && user.getBannedUntil() != null
                && !user.getBannedUntil().isAfter(now);
    }

    private void clearBan(User user) {
        user.setStatus(0);
        user.setBanReason(null);
        user.setBannedBy(null);
        user.setBannedAt(null);
        user.setBannedUntil(null);
        user.setUpdatedAt(LocalDateTime.now());
    }

    private User copyBanState(User user) {
        User copy = new User();
        copy.setId(user.getId());
        copy.setStatus(user.getStatus());
        copy.setBanReason(user.getBanReason());
        copy.setBannedBy(user.getBannedBy());
        copy.setBannedAt(user.getBannedAt());
        copy.setBannedUntil(user.getBannedUntil());
        return copy;
    }
}
