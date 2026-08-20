package com.artiv.service;

import com.artiv.dto.admin.AdminProjectModerationRequest;
import com.artiv.entity.Project;

import java.util.Map;

public interface AdminProjectService {
    Map<String, Object> list(long page, long size, Integer status, Integer moderationStatus, String keyword);
    Map<String, Object> detail(Long id);
    Project moderate(Long adminId, Long id, AdminProjectModerationRequest request);
}
