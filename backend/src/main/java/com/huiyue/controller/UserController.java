package com.huiyue.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.huiyue.common.Result;
import com.huiyue.entity.User;
import com.huiyue.mapper.UserMapper;
import com.huiyue.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 用户模块：个人资料与画师认证
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;

    /** 申请画师认证：提交后进入审核中（cert_status=1） */
    @PutMapping("/me/cert")
    public Result<User> applyCert() {
        Long userId = UserContext.getUserId();
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getCertStatus, 1)
                .set(User::getUpdatedAt, LocalDateTime.now()));
        User user = userMapper.selectById(userId);
        user.setPassword(null);
        return Result.ok(user);
    }

    /** 查看用户公开资料 */
    @GetMapping("/{id}")
    public Result<User> detail(@org.springframework.web.bind.annotation.PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.ok(user);
    }
}
