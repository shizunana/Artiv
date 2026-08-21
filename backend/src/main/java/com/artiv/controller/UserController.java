package com.artiv.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.common.Result;
import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.PainterProfileUpdateRequest;
import com.artiv.dto.AvatarUpdateRequest;
import com.artiv.dto.ArtistSummary;
import com.artiv.dto.PublicProfileResponse;
import com.artiv.dto.admin.CertificationSubmitRequest;
import com.artiv.entity.User;
import com.artiv.entity.Work;
import com.artiv.entity.Stall;
import com.artiv.entity.Project;
import com.artiv.mapper.UserMapper;
import com.artiv.mapper.WorkMapper;
import com.artiv.security.UserContext;
import com.artiv.service.ArtistService;
import com.artiv.service.PublicProfileService;
import com.artiv.service.CertificationService;
import com.artiv.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/**
 * 用户模块：个人资料与画师认证
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final UserService userService;
    private final ArtistService artistService;
    private final PublicProfileService publicProfileService;
    private final CertificationService certificationService;

    /** 画师大厅：仅展示账号正常的已认证画师，并携带公开作品预览。 */
    @GetMapping("/artists")
    public Result<Page<ArtistSummary>> artists(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subjects,
            @RequestParam(required = false) String keyword) {
        return Result.ok(artistService.listArtists(page, size, category, subjects, keyword));
    }

    /** 公开主页资料：只返回可公开展示字段。 */
    @GetMapping("/{id}/public-profile")
    public Result<PublicProfileResponse> publicProfile(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(defaultValue = "painter") String role) {
        return Result.ok(publicProfileService.getProfile(id, role));
    }

    /** 指定用户的公开作品。 */
    @GetMapping("/{id}/works")
    public Result<Page<Work>> publicWorks(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return Result.ok(publicProfileService.listWorks(id, page, size));
    }

    /** 指定画师当前可公开访问的橱窗。 */
    @GetMapping("/{id}/stalls")
    public Result<Page<Stall>> publicStalls(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return Result.ok(publicProfileService.listStalls(id, page, size));
    }

    /** 指定企划方公开发布或已完成的企划。 */
    @GetMapping("/{id}/projects")
    public Result<Page<Project>> publicProjects(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "12") long size) {
        return Result.ok(publicProfileService.listProjects(id, page, size));
    }

    /** @deprecated 兼容旧客户端；新客户端使用 POST /users/me/certifications。 */
    @Deprecated
    @PutMapping("/me/cert")
    public Result<User> applyCert() {
        Long userId = UserContext.getUserId();
        User current = userMapper.selectById(userId);
        if (current == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        CertificationSubmitRequest request = new CertificationSubmitRequest();
        request.setCommonId(current.getNickname());
        certificationService.submit(userId, request);
        User user = userMapper.selectById(userId);
        user.setPassword(null);
        return Result.ok(user);
    }

    /** 画师维护昵称、主页简介和题材风格；认证前也必须能完善申请资料。 */
    @PutMapping("/me/profile")
    public Result<User> updateMyPainterProfile(@Valid @RequestBody PainterProfileUpdateRequest request) {
        return Result.ok(userService.updatePainterProfile(UserContext.getUserId(), request));
    }

    /** 当前登录用户更新头像；头像必须来自本站上传目录。 */
    @PutMapping("/me/avatar")
    public Result<User> updateMyAvatar(@Valid @RequestBody AvatarUpdateRequest request) {
        Long userId = UserContext.getUserId();
        User current = userMapper.selectById(userId);
        if (current == null || Integer.valueOf(1).equals(current.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        String avatar = request.getAvatar().trim();
        if (!avatar.startsWith("/uploads/") || avatar.contains("..")) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "头像必须使用本站上传文件");
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getAvatar, avatar)
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
