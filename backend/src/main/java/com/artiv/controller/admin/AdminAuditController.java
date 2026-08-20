package com.artiv.controller.admin;

import com.artiv.common.Result;
import com.artiv.entity.AdminActionLog;
import com.artiv.service.AdminAuditService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditController {
    private final AdminAuditService adminAuditService;

    @GetMapping
    public Result<Page<AdminActionLog>> list(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "20") long size,
                                             @RequestParam(required = false) String actionType,
                                             @RequestParam(required = false) String targetType,
                                             @RequestParam(required = false) Long adminId) {
        return Result.ok(adminAuditService.list(page, size, actionType, targetType, adminId));
    }
}
