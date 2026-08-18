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
import com.artiv.entity.User;
import com.artiv.entity.Work;
import com.artiv.entity.Stall;
import com.artiv.entity.Project;
import com.artiv.mapper.UserMapper;
import com.artiv.mapper.WorkMapper;
import com.artiv.security.UserContext;
import com.artiv.service.ArtistService;
import com.artiv.service.PublicProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/**
 * 用户模块：个人资料与画师认证
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private static final Set<String> SUBJECT_OPTIONS = Set.of(
            "LOGO", "Q版", "lolita", "安头", "拆分", "场景", "定制", "动图", "氛围", "搞笑",
            "古风", "黑白", "横插", "厚涂", "可爱", "立绘", "模板", "男性", "女性", "排版",
            "签名", "日常", "日系", "色块", "竖插", "双人", "水印", "像素", "小动物", "小元素",
            "有背景", "正比", "周边制品", "桌宠", "字体设计"
    );

    private final UserMapper userMapper;
    private final WorkMapper workMapper;
    private final ArtistService artistService;
    private final PublicProfileService publicProfileService;

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

    /** 申请画师认证：提交后进入审核中（cert_status=1） */
    @PutMapping("/me/cert")
    public Result<User> applyCert() {
        Long userId = UserContext.getUserId();
        User current = userMapper.selectById(userId);
        // 仅未认证（0）或已驳回（3）可申请；已认证（2）或审核中（1）不允许重复/降级申请
        if (current.getCertStatus() != null
                && (current.getCertStatus() == 1 || current.getCertStatus() == 2)) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID);
        }
        Long publicWorkCount = workMapper.selectCount(new LambdaQueryWrapper<Work>()
                .eq(Work::getUserId, userId)
                .eq(Work::getVisibility, 1)
                .eq(Work::getDeleted, 0));
        if (publicWorkCount == null || publicWorkCount < 4) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "申请画师认证前至少需要公开 4 件作品");
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getCertStatus, 1)
                .set(User::getUpdatedAt, LocalDateTime.now()));
        User user = userMapper.selectById(userId);
        user.setPassword(null);
        return Result.ok(user);
    }

    /** 已认证画师仅可维护自己的个人简介和题材风格标签。 */
    @PutMapping("/me/profile")
    public Result<User> updateMyPainterProfile(@Valid @RequestBody PainterProfileUpdateRequest request) {
        Long userId = UserContext.getUserId();
        User current = userMapper.selectById(userId);
        if (current == null || Integer.valueOf(1).equals(current.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (!Integer.valueOf(2).equals(current.getRole()) || !Integer.valueOf(2).equals(current.getCertStatus())) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "仅已认证画师可以修改展示资料");
        }

        List<String> requestedTags = request.getTags() == null ? List.of() : request.getTags();
        LinkedHashSet<String> uniqueTags = new LinkedHashSet<>();
        for (String tag : requestedTags) {
            if (tag == null || !SUBJECT_OPTIONS.contains(tag.trim())) {
                throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "风格标签必须从题材风格选项中选择");
            }
            uniqueTags.add(tag.trim());
        }

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getBio, request.getBio() == null ? "" : request.getBio().trim())
                .set(User::getTags, String.join(",", uniqueTags))
                .set(User::getUpdatedAt, LocalDateTime.now()));
        User user = userMapper.selectById(userId);
        user.setPassword(null);
        return Result.ok(user);
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
