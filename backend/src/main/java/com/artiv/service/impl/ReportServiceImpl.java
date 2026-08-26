package com.artiv.service.impl;

import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.ReportCreateRequest;
import com.artiv.dto.ReportSubmitResponse;
import com.artiv.dto.admin.AdminReportCaseItem;
import com.artiv.dto.admin.AdminReportDetail;
import com.artiv.dto.admin.AdminReportSubmissionItem;
import com.artiv.entity.ReportCase;
import com.artiv.entity.ReportEvidence;
import com.artiv.entity.ReportSubmission;
import com.artiv.entity.User;
import com.artiv.mapper.ReportCaseMapper;
import com.artiv.mapper.ReportEvidenceMapper;
import com.artiv.mapper.ReportSubmissionMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.service.AccountRestrictionService;
import com.artiv.service.AdminAuditService;
import com.artiv.service.ReportService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private static final String TARGET_USER = "USER";
    private static final int PENDING = 0;
    private static final int HANDLED = 1;
    private static final int REJECTED = 2;
    private static final List<String> CATEGORIES = List.of(
            "HARASSMENT", "FRAUD", "IMPERSONATION", "ILLEGAL_CONTENT", "OTHER");

    private final ReportCaseMapper reportCaseMapper;
    private final ReportSubmissionMapper submissionMapper;
    private final ReportEvidenceMapper evidenceMapper;
    private final UserMapper userMapper;
    private final AdminAuditService adminAuditService;
    private final AccountRestrictionService accountRestrictionService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ReportSubmitResponse submit(Long reporterId, ReportCreateRequest request) {
        accountRestrictionService.requireAvailable(reporterId, "举报用户");
        if (reporterId.equals(request.getTargetUserId())) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "不能举报自己");
        }
        String category = request.getCategory().trim().toUpperCase();
        if (!CATEGORIES.contains(category)) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "举报类型不正确");
        }
        String reason = request.getReason().trim();
        List<String> images = normalizeImages(request.getImages());
        User target = userMapper.selectById(request.getTargetUserId());
        if (target == null || Integer.valueOf(1).equals(target.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        ReportCase reportCase = findCase(target.getId());
        if (reportCase == null) {
            reportCase = new ReportCase();
            reportCase.setTargetType(TARGET_USER);
            reportCase.setTargetId(target.getId());
            reportCase.setTargetSnapshot(targetSnapshot(target));
            reportCase.setStatus(PENDING);
            reportCase.setCreatedAt(now);
            reportCase.setUpdatedAt(now);
            try {
                reportCaseMapper.insert(reportCase);
            } catch (DuplicateKeyException ignored) {
                reportCase = findCase(target.getId());
            }
        }

        ReportSubmission existing = submissionMapper.selectOne(new LambdaQueryWrapper<ReportSubmission>()
                .eq(ReportSubmission::getCaseId, reportCase.getId())
                .eq(ReportSubmission::getReporterId, reporterId).last("LIMIT 1"));
        if (existing != null) {
            return new ReportSubmitResponse(reportCase.getId(), false, countReporters(reportCase.getId()));
        }

        ReportSubmission submission = new ReportSubmission();
        submission.setCaseId(reportCase.getId());
        submission.setReporterId(reporterId);
        submission.setCategory(category);
        submission.setReason(reason);
        submission.setCreatedAt(now);
        try {
            submissionMapper.insert(submission);
        } catch (DuplicateKeyException ignored) {
            return new ReportSubmitResponse(reportCase.getId(), false, countReporters(reportCase.getId()));
        }
        for (int index = 0; index < images.size(); index++) {
            ReportEvidence evidence = new ReportEvidence();
            evidence.setSubmissionId(submission.getId());
            evidence.setImageUrl(images.get(index));
            evidence.setSortOrder(index);
            evidence.setCreatedAt(now);
            evidenceMapper.insert(evidence);
        }
        LambdaUpdateWrapper<ReportCase> caseUpdate = new LambdaUpdateWrapper<ReportCase>()
                .eq(ReportCase::getId, reportCase.getId())
                .set(ReportCase::getUpdatedAt, now);
        if (!Integer.valueOf(PENDING).equals(reportCase.getStatus())) {
            caseUpdate.set(ReportCase::getStatus, PENDING)
                    .set(ReportCase::getAdminId, null)
                    .set(ReportCase::getDecisionReason, null)
                    .set(ReportCase::getHandledAt, null);
        }
        reportCaseMapper.update(null, caseUpdate);
        return new ReportSubmitResponse(reportCase.getId(), true, countReporters(reportCase.getId()));
    }

    @Override
    public Page<AdminReportCaseItem> listAdmin(long page, long size, Integer status, String keyword,
                                               LocalDate startDate, LocalDate endDate) {
        validateFilters(status, startDate, endDate);
        Page<ReportCase> source = reportCaseMapper.selectAdminPage(
                new Page<>(Math.max(1, page), Math.min(100, Math.max(1, size))), status,
                StringUtils.hasText(keyword) ? keyword.trim() : null,
                startDate == null ? null : startDate.atStartOfDay(),
                endDate == null ? null : endDate.plusDays(1).atStartOfDay());
        Page<AdminReportCaseItem> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(toCaseItems(source.getRecords()));
        return result;
    }

    @Override
    public AdminReportDetail getAdminDetail(Long caseId) {
        ReportCase reportCase = reportCaseMapper.selectById(caseId);
        if (reportCase == null) throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        return toDetail(reportCase);
    }

    @Override
    @Transactional
    public AdminReportDetail resolve(Long caseId, Long adminId, boolean handled, String reason) {
        ReportCase reportCase = reportCaseMapper.selectByIdForUpdate(caseId);
        if (reportCase == null) throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        if (!Integer.valueOf(PENDING).equals(reportCase.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "该举报案件已经处理");
        }
        String decision = reason == null ? "" : reason.trim();
        if (decision.isEmpty() || decision.length() > 500) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "必须填写 500 字以内的处理说明");
        }
        long reporterCount = countReporters(caseId);
        Map<String, Object> before = Map.of("status", reportCase.getStatus(), "reporterCount", reporterCount);
        reportCase.setStatus(handled ? HANDLED : REJECTED);
        reportCase.setAdminId(adminId);
        reportCase.setDecisionReason(decision);
        reportCase.setHandledAt(LocalDateTime.now());
        reportCase.setUpdatedAt(LocalDateTime.now());
        reportCaseMapper.updateById(reportCase);
        adminAuditService.record(adminId, handled ? "REPORT_HANDLED" : "REPORT_REJECTED", "REPORT_CASE",
                caseId, before, Map.of("status", reportCase.getStatus(), "reporterCount", reporterCount), decision);
        return toDetail(reportCase);
    }

    @Override
    public long countPending() {
        Long count = reportCaseMapper.selectCount(new LambdaQueryWrapper<ReportCase>()
                .eq(ReportCase::getStatus, PENDING));
        return count == null ? 0 : count;
    }

    private ReportCase findCase(Long targetId) {
        return reportCaseMapper.selectOne(new LambdaQueryWrapper<ReportCase>()
                .eq(ReportCase::getTargetType, TARGET_USER)
                .eq(ReportCase::getTargetId, targetId).last("LIMIT 1"));
    }

    private AdminReportDetail toDetail(ReportCase reportCase) {
        AdminReportCaseItem base = toCaseItems(List.of(reportCase)).get(0);
        AdminReportDetail detail = new AdminReportDetail();
        detail.setReportCase(base.getReportCase());
        detail.setTargetNickname(base.getTargetNickname());
        detail.setTargetAvatar(base.getTargetAvatar());
        detail.setTargetStatus(base.getTargetStatus());
        detail.setReporterCount(base.getReporterCount());
        List<ReportSubmission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<ReportSubmission>()
                .eq(ReportSubmission::getCaseId, reportCase.getId())
                .orderByDesc(ReportSubmission::getCreatedAt).orderByDesc(ReportSubmission::getId));
        Map<Long, User> reporters = loadUsers(submissions.stream().map(ReportSubmission::getReporterId).toList());
        List<Long> submissionIds = submissions.stream().map(ReportSubmission::getId).toList();
        Map<Long, List<String>> images = submissionIds.isEmpty() ? Map.of() : evidenceMapper.selectList(
                        new LambdaQueryWrapper<ReportEvidence>().in(ReportEvidence::getSubmissionId, submissionIds)
                                .orderByAsc(ReportEvidence::getSortOrder).orderByAsc(ReportEvidence::getId))
                .stream().collect(Collectors.groupingBy(ReportEvidence::getSubmissionId, LinkedHashMap::new,
                        Collectors.mapping(ReportEvidence::getImageUrl, Collectors.toList())));
        detail.setSubmissions(submissions.stream().map(submission -> {
            User reporter = reporters.get(submission.getReporterId());
            AdminReportSubmissionItem item = new AdminReportSubmissionItem();
            item.setSubmission(submission);
            item.setReporterNickname(displayName(reporter));
            item.setReporterAvatar(reporter == null ? null : reporter.getAvatar());
            item.setImages(images.getOrDefault(submission.getId(), List.of()));
            return item;
        }).toList());
        return detail;
    }

    private List<AdminReportCaseItem> toCaseItems(List<ReportCase> cases) {
        if (cases.isEmpty()) return List.of();
        Map<Long, User> targets = loadUsers(cases.stream().map(ReportCase::getTargetId).toList());
        List<AdminReportCaseItem> result = new ArrayList<>();
        for (ReportCase reportCase : cases) {
            User target = targets.get(reportCase.getTargetId());
            AdminReportCaseItem item = new AdminReportCaseItem();
            item.setReportCase(reportCase);
            item.setTargetNickname(displayName(target));
            item.setTargetAvatar(target == null ? null : target.getAvatar());
            item.setTargetStatus(target == null ? null : target.getStatus());
            item.setReporterCount(countReporters(reportCase.getId()));
            result.add(item);
        }
        return result;
    }

    private Map<Long, User> loadUsers(List<Long> ids) {
        List<Long> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) return Map.of();
        return userMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private long countReporters(Long caseId) {
        Long count = submissionMapper.selectCount(new LambdaQueryWrapper<ReportSubmission>()
                .eq(ReportSubmission::getCaseId, caseId));
        return count == null ? 0 : count;
    }

    private void validateFilters(Integer status, LocalDate startDate, LocalDate endDate) {
        if (status != null && (status < PENDING || status > REJECTED)) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
    }

    private List<String> normalizeImages(List<String> values) {
        if (values == null || values.isEmpty()) return List.of();
        if (values.size() > 6) throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "举报证据最多 6 张");
        return values.stream().map(value -> {
            String image = value == null ? "" : value.trim();
            if (!image.startsWith("/uploads/") || image.contains("..") || image.length() > 255) {
                throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "举报图片必须使用本站上传文件");
            }
            return image;
        }).distinct().toList();
    }

    private String targetSnapshot(User user) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "nickname", displayName(user),
                    "username", user.getUsername(),
                    "avatar", user.getAvatar() == null ? "" : user.getAvatar()));
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private String displayName(User user) {
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) return "已注销用户";
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }
}
