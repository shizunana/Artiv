package com.huiyue.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huiyue.common.BusinessException;
import com.huiyue.common.ResultCode;
import com.huiyue.dto.LoginRequest;
import com.huiyue.dto.RegisterRequest;
import com.huiyue.entity.User;
import com.huiyue.mapper.UserMapper;
import com.huiyue.security.JwtUtil;
import com.huiyue.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public User register(RegisterRequest request) {
        Long exists = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, request.getUsername()));
        if (exists > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setRole(request.getRole());
        user.setStatus(0);
        user.setDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        user.setPassword(null);
        return user;
    }

    @Override
    public Map<String, Object> login(LoginRequest request) {
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "账号已被封禁");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        user.setPassword(null);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);
        return data;
    }

    @Override
    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        user.setPassword(null);
        return user;
    }
}
