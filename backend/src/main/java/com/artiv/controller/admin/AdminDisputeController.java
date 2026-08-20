package com.artiv.controller.admin;

import com.artiv.common.Result;
import com.artiv.dto.admin.AdminArbitrationRequest;
import com.artiv.security.UserContext;
import com.artiv.service.DisputeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/disputes")
@RequiredArgsConstructor
public class AdminDisputeController {
    private final DisputeService disputeService;

    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "20") long size,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(required = false) String keyword) {
        return Result.ok(disputeService.listAdmin(page, size, status, keyword));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(disputeService.detailAdmin(id));
    }

    @PostMapping("/{id}/arbitrate")
    public Result<Map<String, Object>> arbitrate(@PathVariable Long id,
                                                 @Valid @RequestBody AdminArbitrationRequest request) {
        return Result.ok(disputeService.arbitrate(UserContext.getUserId(), id, request));
    }
}
