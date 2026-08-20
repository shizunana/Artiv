package com.artiv.controller;

import com.artiv.common.Result;
import com.artiv.dto.admin.CertificationSubmitRequest;
import com.artiv.entity.ArtistCertification;
import com.artiv.security.UserContext;
import com.artiv.service.CertificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/certifications")
@RequiredArgsConstructor
public class CertificationController {
    private final CertificationService certificationService;

    @PostMapping
    public Result<ArtistCertification> submit(@Valid @RequestBody CertificationSubmitRequest request) {
        return Result.ok(certificationService.submit(UserContext.getUserId(), request));
    }

    @GetMapping("/latest")
    public Result<ArtistCertification> latest() {
        return Result.ok(certificationService.latest(UserContext.getUserId()));
    }
}
