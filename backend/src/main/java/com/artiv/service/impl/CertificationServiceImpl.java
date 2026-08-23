package com.artiv.service.impl;

import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.CertificationChangeRequest;
import com.artiv.dto.CertificationStatusResponse;
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
    private static final int TYPE_INITIAL = 0;
    private static final int TYPE_PROFILE_CHANGE = 1;
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;
    private static final Set<String> SUPPORTED_SITE_DOMAINS = Set.of(
            "pixiv.net", "lofter.com", "weibo.com", "xiaohongshu.com",
            "bilibili.com", "deviantart.com", "zcool.com.cn", "gracg.com"
    );
    private final ArtistCertificationMapper certificationMapper;
    private final UserMapper userMapper;
    private final WorkMapper workMapper;
    private final MessageService messageService;
    private final AdminAuditService auditService;

    @Override
    @Transactional
    public ArtistCertification submit(Long userId, CertificationSubmitRequest request) {
        User user = requireApplicantForUpdate(userId);
        if (Integer.valueOf(1).equals(user.getCertStatus()) || Integer.valueOf(2).equals(user.getCertStatus())
                || hasPending(userId, TYPE_INITIAL)) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "当前认证状态不可重复申请");
        }
        if (user.getBio() == null || user.getBio().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "请先完善主页简介");
        }
        long publicWorks = countPublicWorks(userId);
        if (publicWorks < 4) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "申请画师认证前至少需要公开 4 件作品");
        }
        String commonId = normalizeCommonId(request.getCommonId());
        String site = normalizeSite(request.getCommonSite());

        ArtistCertification certification = new ArtistCertification();
        certification.setUserId(userId);
        certification.setApplicationType(TYPE_INITIAL);
        certification.setCommonId(commonId);
        certification.setCommonSite(site);
        certification.setPublicWorkCount(Math.toIntExact(publicWorks));
        certification.setStatus(STATUS_PENDING);
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
    @Transactional
    public ArtistCertification submitChange(Long userId, CertificationChangeRequest request) {
        User user = requireApplicantForUpdate(userId);
        if (!Integer.valueOf(2).equals(user.getCertStatus())) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "仅已认证用户可修改认证资料");
        }
        if (hasPending(userId, TYPE_PROFILE_CHANGE)) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "已有认证资料修改正在审核");
        }
        String commonId = normalizeCommonId(request.getCommonId());
        String site = normalizeSite(request.getCommonSite());
        if (commonId.equals(user.getArtistName()) && site.equals(user.getArtistSite())) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "笔名和常用站均未发生变化");
        }

        ArtistCertification certification = new ArtistCertification();
        certification.setUserId(userId);
        certification.setApplicationType(TYPE_PROFILE_CHANGE);
        certification.setCommonId(commonId);
        certification.setCommonSite(site);
        certification.setPreviousCommonId(user.getArtistName());
        certification.setPreviousCommonSite(user.getArtistSite());
        certification.setPublicWorkCount(Math.toIntExact(countPublicWorks(userId)));
        certification.setStatus(STATUS_PENDING);
        certification.setDeleted(0);
        certification.setCreatedAt(LocalDateTime.now());
        certification.setUpdatedAt(LocalDateTime.now());
        certificationMapper.insert(certification);
        return certification;
    }

    @Override
    public CertificationStatusResponse status(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted()) || Integer.valueOf(3).equals(user.getRole())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        long publicWorkCount = countPublicWorks(userId);
        CertificationStatusResponse response = new CertificationStatusResponse();
        response.setCertStatus(Integer.valueOf(3).equals(user.getCertStatus()) ? 0 : user.getCertStatus());
        response.setArtistName(user.getArtistName());
        response.setArtistSite(user.getArtistSite());
        response.setPainterBio(user.getBio());
        response.setTags(user.getTags());
        response.setPublicWorkCount(publicWorkCount);
        response.setPainterBioCompleted(user.getBio() != null && !user.getBio().isBlank());
        response.setEligibleForInitialApplication(response.isPainterBioCompleted() && publicWorkCount >= 4
                && Integer.valueOf(0).equals(response.getCertStatus()));
        response.setPendingInitialApplication(findPending(userId, TYPE_INITIAL));
        response.setPendingChangeApplication(findPending(userId, TYPE_PROFILE_CHANGE));
        return response;
    }

    @Override
    public ArtistCertification latest(Long userId) {
        return certificationMapper.selectOne(new LambdaQueryWrapper<ArtistCertification>()
                .eq(ArtistCertification::getUserId, userId)
                .orderByDesc(ArtistCertification::getCreatedAt)
                .last("LIMIT 1"));
    }

    @Override
    public Map<String, Object> listAdmin(long page, long size, Integer status, Integer applicationType, String keyword) {
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
                .eq(applicationType != null, ArtistCertification::getApplicationType, applicationType)
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
                .eq(Work::getDeleted, 0)
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
        int applicationType = certification.getApplicationType() == null ? TYPE_INITIAL : certification.getApplicationType();
        if (applicationType == TYPE_INITIAL && !Integer.valueOf(1).equals(user.getCertStatus())) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "用户当前已不在初次认证审核中");
        }
        if (applicationType == TYPE_INITIAL && approved
                && ((user.getBio() == null || user.getBio().isBlank()) || countPublicWorks(user.getId()) < 4)) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "当前主页简介或公开作品数量已不满足认证要求");
        }
        if (applicationType == TYPE_PROFILE_CHANGE && !Integer.valueOf(2).equals(user.getCertStatus())) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "用户当前已不是已认证状态");
        }
        if (approved) {
            certification.setCommonId(normalizeCommonId(certification.getCommonId()));
            certification.setCommonSite(normalizeSite(certification.getCommonSite()));
        }

        ArtistCertification before = new ArtistCertification();
        before.setId(certification.getId());
        before.setStatus(certification.getStatus());
        certification.setStatus(approved ? STATUS_APPROVED : STATUS_REJECTED);
        certification.setReviewerId(adminId);
        certification.setReviewReason(reason.trim());
        certification.setReviewedAt(LocalDateTime.now());
        certification.setUpdatedAt(LocalDateTime.now());
        certificationMapper.updateById(certification);

        if (applicationType == TYPE_INITIAL) {
            user.setCertStatus(approved ? 2 : 0);
        }
        if (approved) {
            user.setArtistName(certification.getCommonId());
            user.setArtistSite(certification.getCommonSite());
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        String subtype;
        String content;
        if (applicationType == TYPE_PROFILE_CHANGE) {
            subtype = approved ? "CERTIFICATION_PROFILE_CHANGE_APPROVED" : "CERTIFICATION_PROFILE_CHANGE_REJECTED";
            content = approved ? "您的画师认证资料修改已通过审核。"
                    : "您的画师认证资料修改未通过审核。原因：" + reason.trim();
        } else {
            subtype = approved ? "CERTIFICATION_APPROVED" : "CERTIFICATION_REJECTED";
            content = approved ? "您的画师认证申请已通过。"
                    : "您的画师认证申请未通过。原因：" + reason.trim();
        }
        messageService.createNotification(user.getId(), 0, subtype, null, content);
        auditService.record(adminId, approved ? "APPROVE_CERTIFICATION" : "REJECT_CERTIFICATION",
                "ARTIST_CERTIFICATION", certification.getId(), before, certification, reason.trim());
        return certification;
    }

    private User requireApplicantForUpdate(Long userId) {
        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (Integer.valueOf(3).equals(user.getRole())) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "管理员不能申请画师认证");
        }
        if (!Integer.valueOf(0).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "当前账号状态不能提交认证申请");
        }
        return user;
    }

    private boolean hasPending(Long userId, int applicationType) {
        return certificationMapper.selectCount(new LambdaQueryWrapper<ArtistCertification>()
                .eq(ArtistCertification::getUserId, userId)
                .eq(ArtistCertification::getApplicationType, applicationType)
                .eq(ArtistCertification::getStatus, STATUS_PENDING)
                .eq(ArtistCertification::getDeleted, 0)) > 0;
    }

    private ArtistCertification findPending(Long userId, int applicationType) {
        return certificationMapper.selectOne(new LambdaQueryWrapper<ArtistCertification>()
                .eq(ArtistCertification::getUserId, userId)
                .eq(ArtistCertification::getApplicationType, applicationType)
                .eq(ArtistCertification::getStatus, STATUS_PENDING)
                .eq(ArtistCertification::getDeleted, 0)
                .orderByDesc(ArtistCertification::getCreatedAt)
                .last("LIMIT 1"));
    }

    private long countPublicWorks(Long userId) {
        return workMapper.selectCount(new LambdaQueryWrapper<Work>()
                .eq(Work::getUserId, userId)
                .eq(Work::getVisibility, 1)
                .eq(Work::getDeleted, 0));
    }

    private String normalizeCommonId(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty() || value.length() > 100) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "笔名不能为空且不能超过 100 字");
        }
        return value;
    }

    private String normalizeSite(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "常用站不能为空");
        }
        try {
            URI uri = URI.create(raw.trim());
            if (("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null) {
                String host = uri.getHost().toLowerCase();
                boolean supported = SUPPORTED_SITE_DOMAINS.stream()
                        .anyMatch(domain -> host.equals(domain) || host.endsWith("." + domain));
                if (!supported) {
                    throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "常用站仅支持指定创作平台");
                }
                String path = uri.getPath();
                boolean hasSpecificPath = path != null && !path.isBlank() && !"/".equals(path);
                boolean personalSubdomain = (host.endsWith(".lofter.com")
                        && !Set.of("www.lofter.com", "m.lofter.com").contains(host))
                        || (host.endsWith(".deviantart.com") && !"www.deviantart.com".equals(host));
                if (!hasSpecificPath && !personalSubdomain) {
                    throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "常用站必须填写具体个人主页或作品集地址");
                }
                return uri.normalize().toString();
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (IllegalArgumentException ignored) {
            // 统一返回业务错误。
        }
        throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "常用站必须是有效的 http/https 地址");
    }
}
