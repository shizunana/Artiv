package com.artiv.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.LoginRequest;
import com.artiv.dto.RegisterRequest;
import com.artiv.entity.User;
import com.artiv.mapper.UserMapper;
import com.artiv.security.JwtUtil;
import com.artiv.service.UserService;
import com.artiv.service.AccountRestrictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
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
    private final AccountRestrictionService accountRestrictionService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public User register(RegisterRequest request) {
        String nickname = request.getNickname().trim();
        Long exists = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, request.getUsername()));
        if (exists > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        if (nicknameExists(nickname)) {
            throw new BusinessException(ResultCode.NICKNAME_EXISTS);
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(nickname);
        user.setRole(request.getRole());
        user.setCertStatus(0);
        user.setStatus(0);
        user.setDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException duplicateKeyException) {
            // 预检查不能替代数据库唯一索引；并发同昵称注册时转换为稳定业务错误。
            if (nicknameExists(nickname)) {
                throw new BusinessException(ResultCode.NICKNAME_EXISTS);
            }
            throw duplicateKeyException;
        }
        user.setPassword(null);
        return user;
    }

    private boolean nicknameExists(String nickname) {
        return userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getNickname, nickname)) > 0;
    }

    @Override
    public Map<String, Object> login(LoginRequest request) {
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }
        user = accountRestrictionService.refresh(user.getId());
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        user.setPassword(null);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);
        return data;
    }

    @Override
    public User getById(Long id) {
        User user = accountRestrictionService.refresh(id);
        user.setPassword(null);
        return user;
    }
}
