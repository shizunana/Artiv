package com.artiv.controller.admin;

import com.artiv.common.Result;
import com.artiv.dto.admin.AdminProjectModerationRequest;
import com.artiv.entity.Project;
import com.artiv.security.UserContext;
import com.artiv.service.AdminProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/projects")
@RequiredArgsConstructor
public class AdminProjectController {
    private final AdminProjectService adminProjectService;

    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "20") long size,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(required = false) Integer moderationStatus,
                                            @RequestParam(required = false) String keyword) {
        return Result.ok(adminProjectService.list(page, size, status, moderationStatus, keyword));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(adminProjectService.detail(id));
    }

    @PutMapping("/{id}/moderation")
    public Result<Project> moderate(@PathVariable Long id,
                                     @Valid @RequestBody AdminProjectModerationRequest request) {
        return Result.ok(adminProjectService.moderate(UserContext.getUserId(), id, request));
    }
}
