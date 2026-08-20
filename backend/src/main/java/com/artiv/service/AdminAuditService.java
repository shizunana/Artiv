package com.artiv.service;

import com.artiv.entity.AdminActionLog;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface AdminAuditService {
    void record(Long adminId, String actionType, String targetType, Long targetId,
                Object beforeData, Object afterData, String reason);

    Page<AdminActionLog> list(long page, long size, String actionType, String targetType, Long adminId);
}
