package com.artiv.service.impl;

import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.admin.CertificationSubmitRequest;
import com.artiv.entity.ArtistCertification;
import com.artiv.entity.User;
import com.artiv.entity.Work;
import com.artiv.mapper.ArtistCertificationMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.mapper.WorkMapper;
import com.artiv.service.AdminAuditService;
import com.artiv.service.CertificationService;
import com.artiv.service.MessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CertificationServiceImpl implements CertificationService {
    private final ArtistCertificationMapper certificationMapper;
    private final UserMapper userMapper;
    private final WorkMapper workMapper;
    private final MessageService messageService;
    private final AdminAuditService auditService;

    @Override
    @Transactional
    public ArtistCertification submit(Long userId, CertificationSubmitRequest request) {
        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null || !Integer.valueOf(2).equals(user.getRole())) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "仅画师账号可提交认证");
        }
        if (Integer.valueOf(1).equals(user.getCertStatus()) || Integer.valueOf(2).equals(user.getCertStatus())) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "当前认证状态不可重复申请");
        }
        if (user.getBio() == null || user.getBio().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "请先完善主页简介");
        }
        long publicWorks = countPublicWorks(userId);
        if (publicWorks < 4) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "申请画师认证前至少需要公开 4 件作品");
        }
        String site = normalizeSite(request.getCommonSite());

        ArtistCertification certification = new ArtistCertification();
        certification.setUserId(userId);
        certification.setCommonId(request.getCommonId().trim());
        certification.setCommonSite(site);
        certification.setPublicWorkCount(Math.toIntExact(publicWorks));
        certification.setStatus(0);
        certification.setDeleted(0);
        certification.setCreatedAt(LocalDateTime.now());
        certification.setUpdatedAt(LocalDateTime.now());
        certificationMapper.insert(certification);

        user.setCertStatus(1);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return certification;
    }

    @Override
    public ArtistCertification latest(Long userId) {
        return certificationMapper.selectOne(new LambdaQueryWrapper<ArtistCertification>()
                .eq(ArtistCertification::getUserId, userId)
                .orderByDesc(ArtistCertification::getCreatedAt)
                .last("LIMIT 1"));
    }

    @Override
    public Map<String, Object> listAdmin(long page, long size, Integer status, String keyword) {
        Set<Long> matchingUsers = null;
        if (keyword != null && !keyword.isBlank()) {
            String value = keyword.trim();
            matchingUsers = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .and(w -> w.like(User::getUsername, value).or().like(User::getNickname, value)))
                    .stream().map(User::getId).collect(Collectors.toSet());
            if (matchingUsers.isEmpty()) {
                return Map.of("records", List.of(), "total", 0L, "current", page, "size", size);
            }
        }
        LambdaQueryWrapper<ArtistCertification> query = new LambdaQueryWrapper<ArtistCertification>()
                .eq(status != null, ArtistCertification::getStatus, status)
                .in(matchingUsers != null, ArtistCertification::getUserId, matchingUsers)
                .orderByAsc(ArtistCertification::getStatus)
                .orderByDesc(ArtistCertification::getCreatedAt);
        Page<ArtistCertification> result = certificationMapper.selectPage(new Page<>(page, Math.min(size, 100)), query);
        Set<Long> userIds = result.getRecords().stream()
                .map(ArtistCertification::getUserId).collect(Collectors.toSet());
        Map<Long, User> users = userIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(userIds)
                .stream().peek(u -> u.setPassword(null))
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<Map<String, Object>> records = new ArrayList<>();
        for (ArtistCertification certification : result.getRecords()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("certification", certification);
            item.put("user", users.get(certification.getUserId()));
            records.add(item);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("records", records);
        response.put("total", result.getTotal());
        response.put("current", result.getCurrent());
        response.put("size", result.getSize());
        return response;
    }

    @Override
    public Map<String, Object> detailAdmin(Long id) {
        ArtistCertification certification = certificationMapper.selectById(id);
        if (certification == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        User user = userMapper.selectById(certification.getUserId());
        if (user != null) {
            user.setPassword(null);
        }
        List<Work> works = workMapper.selectList(new LambdaQueryWrapper<Work>()
                .eq(Work::getUserId, certification.getUserId())
                .eq(Work::getVisibility, 1)
                .orderByDesc(Work::getCreatedAt));
        List<ArtistCertification> history = certificationMapper.selectList(new LambdaQueryWrapper<ArtistCertification>()
                .eq(ArtistCertification::getUserId, certification.getUserId())
                .orderByDesc(ArtistCertification::getCreatedAt));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("certification", certification);
        response.put("user", user);
        response.put("works", works);
        response.put("history", history);
        return response;
    }

    @Override
    @Transactional
    public ArtistCertification decide(Long adminId, Long id, boolean approved, String reason) {
        ArtistCertification certification = certificationMapper.selectByIdForUpdate(id);
        if (certification == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (!Integer.valueOf(0).equals(certification.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "该申请已经处理");
        }
        User user = userMapper.selectByIdForUpdate(certification.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (approved && ((user.getBio() == null || user.getBio().isBlank()) || countPublicWorks(user.getId()) < 4)) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "当前主页简介或公开作品数量已不满足认证要求");
        }

        ArtistCertification before = new ArtistCertification();
        before.setId(certification.getId());
        before.setStatus(certification.getStatus());
        certification.setStatus(approved ? 1 : 2);
        certification.setReviewerId(adminId);
        certification.setReviewReason(reason.trim());
        certification.setReviewedAt(LocalDateTime.now());
        certification.setUpdatedAt(LocalDateTime.now());
        certificationMapper.updateById(certification);

        user.setCertStatus(approved ? 2 : 3);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        messageService.createNotification(user.getId(), 0,
                approved ? "CERTIFICATION_APPROVED" : "CERTIFICATION_REJECTED", null,
                approved ? "您的画师认证已通过。" : "您的画师认证未通过：" + reason.trim());
        auditService.record(adminId, approved ? "APPROVE_CERTIFICATION" : "REJECT_CERTIFICATION",
                "ARTIST_CERTIFICATION", certification.getId(), before, certification, reason.trim());
        return certification;
    }

    private long countPublicWorks(Long userId) {
        return workMapper.selectCount(new LambdaQueryWrapper<Work>()
                .eq(Work::getUserId, userId)
                .eq(Work::getVisibility, 1));
    }

    private String normalizeSite(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(raw.trim());
            if (("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null) {
                return uri.toString();
            }
        } catch (IllegalArgumentException ignored) {
            // 统一返回业务错误。
        }
        throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "常用站必须是有效的 http/https 个人主页地址");
    }
}
