package com.artiv.controller;

import com.artiv.common.Result;
import com.artiv.dto.ReportCreateRequest;
import com.artiv.dto.ReportSubmitResponse;
import com.artiv.security.UserContext;
import com.artiv.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @PostMapping
    public Result<ReportSubmitResponse> submit(@Valid @RequestBody ReportCreateRequest request) {
        return Result.ok(reportService.submit(UserContext.getUserId(), request));
    }
}
