package com.artiv.controller.admin;

import com.artiv.common.Result;
import com.artiv.dto.admin.AdminUserStatusRequest;
import com.artiv.dto.admin.AdminBalanceAdjustmentRequest;
import com.artiv.dto.admin.AdminBalanceSetRequest;
import com.artiv.security.UserContext;
import com.artiv.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final AdminUserService adminUserService;

    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "20") long size,
                                            @RequestParam(required = false) Integer role,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(required = false) Integer certStatus,
                                            @RequestParam(required = false) String keyword) {
        return Result.ok(adminUserService.list(page, size, role, status, certStatus, keyword));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(adminUserService.detail(id));
    }

    @PutMapping("/{id}/status")
    public Result<Map<String, Object>> changeStatus(@PathVariable Long id,
                                                     @Valid @RequestBody AdminUserStatusRequest request) {
        return Result.ok(adminUserService.changeStatus(UserContext.getUserId(), id, request));
    }

    @PostMapping("/{id}/balance/credit")
    public Result<Map<String, Object>> creditBalance(@PathVariable Long id,
                                                      @Valid @RequestBody AdminBalanceAdjustmentRequest request) {
        return Result.ok(adminUserService.creditBalance(UserContext.getUserId(), id, request));
    }

    @PostMapping("/{id}/balance/adjust")
    public Result<Map<String, Object>> adjustBalance(@PathVariable Long id,
                                                      @Valid @RequestBody AdminBalanceSetRequest request) {
        return Result.ok(adminUserService.adjustBalance(UserContext.getUserId(), id, request));
    }
}
