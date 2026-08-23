package com.artiv.controller.admin;

import com.artiv.common.Result;
import com.artiv.dto.admin.AdminReasonRequest;
import com.artiv.entity.ArtistCertification;
import com.artiv.security.UserContext;
import com.artiv.service.CertificationService;
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
@RequestMapping("/api/v1/admin/certifications")
@RequiredArgsConstructor
public class AdminCertificationController {
    private final CertificationService certificationService;

    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "20") long size,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(required = false) Integer applicationType,
                                            @RequestParam(required = false) String keyword) {
        return Result.ok(certificationService.listAdmin(page, size, status, applicationType, keyword));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(certificationService.detailAdmin(id));
    }

    @PostMapping("/{id}/approve")
    public Result<ArtistCertification> approve(@PathVariable Long id,
                                                @Valid @RequestBody AdminReasonRequest request) {
        return Result.ok(certificationService.decide(UserContext.getUserId(), id, true, request.getReason()));
    }

    @PostMapping("/{id}/reject")
    public Result<ArtistCertification> reject(@PathVariable Long id,
                                               @Valid @RequestBody AdminReasonRequest request) {
        return Result.ok(certificationService.decide(UserContext.getUserId(), id, false, request.getReason()));
    }
}
