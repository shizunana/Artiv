package com.artiv.controller.admin;

import com.artiv.common.Result;
import com.artiv.dto.admin.AdminReportCaseItem;
import com.artiv.dto.admin.AdminReportDecisionRequest;
import com.artiv.dto.admin.AdminReportDetail;
import com.artiv.security.UserContext;
import com.artiv.service.ReportService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {
    private final ReportService reportService;

    @GetMapping
    public Result<Page<AdminReportCaseItem>> list(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "20") long size,
                                                  @RequestParam(required = false) Integer status,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(reportService.listAdmin(page, size, status, keyword, startDate, endDate));
    }

    @GetMapping("/{id}")
    public Result<AdminReportDetail> detail(@PathVariable Long id) {
        return Result.ok(reportService.getAdminDetail(id));
    }

    @PutMapping("/{id}/resolve")
    public Result<AdminReportDetail> resolve(@PathVariable Long id,
                                             @RequestParam boolean handled,
                                             @Valid @RequestBody AdminReportDecisionRequest request) {
        return Result.ok(reportService.resolve(id, UserContext.getUserId(), handled, request.getReason()));
    }
}
