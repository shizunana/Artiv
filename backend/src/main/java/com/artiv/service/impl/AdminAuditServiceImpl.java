package com.artiv.service.impl;

import com.artiv.entity.AdminActionLog;
import com.artiv.mapper.AdminActionLogMapper;
import com.artiv.service.AdminAuditService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAuditServiceImpl implements AdminAuditService {
    private final AdminActionLogMapper logMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void record(Long adminId, String actionType, String targetType, Long targetId,
                       Object beforeData, Object afterData, String reason) {
        AdminActionLog log = new AdminActionLog();
        log.setAdminId(adminId);
        log.setActionType(actionType);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setBeforeData(toJson(beforeData));
        log.setAfterData(toJson(afterData));
        log.setReason(reason);
        log.setRequestId(UUID.randomUUID().toString());
        log.setCreatedAt(LocalDateTime.now());
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            HttpServletRequest request = servletAttributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            log.setIpAddress(forwarded == null || forwarded.isBlank()
                    ? request.getRemoteAddr() : forwarded.split(",")[0].trim());
        }
        logMapper.insert(log);
    }

    @Override
    public Page<AdminActionLog> list(long page, long size, String actionType, String targetType, Long adminId) {
        return logMapper.selectPage(new Page<>(page, Math.min(size, 100)),
                new LambdaQueryWrapper<AdminActionLog>()
                        .eq(actionType != null && !actionType.isBlank(), AdminActionLog::getActionType, actionType)
                        .eq(targetType != null && !targetType.isBlank(), AdminActionLog::getTargetType, targetType)
                        .eq(adminId != null, AdminActionLog::getAdminId, adminId)
                        .orderByDesc(AdminActionLog::getCreatedAt));
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ignored) {
            return String.valueOf(value);
        }
    }
}
