package com.huiyue.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.huiyue.common.Result;
import com.huiyue.common.BusinessException;
import com.huiyue.common.ResultCode;
import com.huiyue.dto.PainterProfileUpdateRequest;
import com.huiyue.dto.ArtistSummary;
import com.huiyue.entity.User;
import com.huiyue.entity.Work;
import com.huiyue.mapper.UserMapper;
import com.huiyue.mapper.WorkMapper;
import com.huiyue.security.UserContext;
import com.huiyue.service.ArtistService;
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

    /** 画师大厅：仅展示账号正常的已认证画师，并携带公开作品预览。 */
    @GetMapping("/artists")
    public Result<List<ArtistSummary>> artists(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subjects,
            @RequestParam(required = false) String keyword) {
        return Result.ok(artistService.listArtists(category, subjects, keyword));
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
