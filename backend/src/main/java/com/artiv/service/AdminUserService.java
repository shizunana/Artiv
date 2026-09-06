package com.artiv.service;

import com.artiv.dto.admin.AdminUserStatusRequest;
import com.artiv.dto.admin.AdminBalanceAdjustmentRequest;
import com.artiv.dto.admin.AdminBalanceSetRequest;
import com.artiv.entity.User;

import java.util.Map;

public interface AdminUserService {
    Map<String, Object> list(long page, long size, Integer role, Integer status, Integer certStatus, String keyword);
    Map<String, Object> detail(Long id);
    Map<String, Object> changeStatus(Long adminId, Long id, AdminUserStatusRequest request);
    Map<String, Object> creditBalance(Long adminId, Long id, AdminBalanceAdjustmentRequest request);
    Map<String, Object> adjustBalance(Long adminId, Long id, AdminBalanceSetRequest request);
}
