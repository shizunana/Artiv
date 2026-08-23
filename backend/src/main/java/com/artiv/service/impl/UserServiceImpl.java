package com.artiv.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.LoginRequest;
import com.artiv.dto.AccountProfileUpdateRequest;
import com.artiv.dto.PasswordUpdateRequest;
import com.artiv.dto.PainterProfileUpdateRequest;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Set<String> SUBJECT_OPTIONS = Set.of(
            "LOGO", "Q版", "lolita", "安头", "拆分", "场景", "定制", "动图", "氛围", "搞笑",
            "古风", "黑白", "横插", "厚涂", "可爱", "立绘", "模板", "男性", "女性", "排版",
            "签名", "日常", "日系", "色块", "竖插", "双人", "水印", "像素", "小动物", "小元素",
            "有背景", "正比", "周边制品", "桌宠", "字体设计"
    );

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
        return nicknameExists(nickname, null);
    }

    private boolean nicknameExists(String nickname, Long excludedUserId) {
        return userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getNickname, nickname)
                .ne(excludedUserId != null, User::getId, excludedUserId)) > 0;
    }

    @Override
    public User updatePainterProfile(Long userId, PainterProfileUpdateRequest request) {
        requireMutableUser(userId);

        List<String> requestedTags = request.getTags() == null ? List.of() : request.getTags();
        LinkedHashSet<String> uniqueTags = new LinkedHashSet<>();
        for (String tag : requestedTags) {
            if (tag == null || !SUBJECT_OPTIONS.contains(tag.trim())) {
                throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "风格标签必须从题材风格选项中选择");
            }
            uniqueTags.add(tag.trim());
        }

        User patch = new User();
        patch.setId(userId);
        patch.setBio(request.getBio() == null ? "" : request.getBio().trim());
        patch.setTags(String.join(",", uniqueTags));
        patch.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(patch);

        User updated = userMapper.selectById(userId);
        updated.setPassword(null);
        return updated;
    }

    @Override
    public User updateAccountProfile(Long userId, AccountProfileUpdateRequest request) {
        requireMutableUser(userId);
        String nickname = request.getNickname().trim();
        if (nicknameExists(nickname, userId)) {
            throw new BusinessException(ResultCode.NICKNAME_EXISTS.getCode(), "昵称已被使用，无法更改");
        }
        try {
            User patch = new User();
            patch.setId(userId);
            patch.setNickname(nickname);
            patch.setEmployerBio(request.getEmployerBio() == null ? "" : request.getEmployerBio().trim());
            patch.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(patch);
        } catch (DuplicateKeyException duplicateKeyException) {
            if (nicknameExists(nickname, userId)) {
                throw new BusinessException(ResultCode.NICKNAME_EXISTS.getCode(), "昵称已被使用，无法更改");
            }
            throw duplicateKeyException;
        }
        User updated = userMapper.selectById(userId);
        updated.setPassword(null);
        return updated;
    }

    @Override
    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        User current = requireMutableUser(userId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), current.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "当前密码不正确");
        }
        if (passwordEncoder.matches(request.getNewPassword(), current.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "新密码不能与当前密码相同");
        }
        User patch = new User();
        patch.setId(userId);
        patch.setPassword(passwordEncoder.encode(request.getNewPassword()));
        patch.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(patch);
    }

    private User requireMutableUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (Integer.valueOf(3).equals(user.getRole())) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "管理员不使用普通账户资料入口");
        }
        return user;
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
