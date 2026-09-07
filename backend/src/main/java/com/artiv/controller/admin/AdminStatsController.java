package com.artiv.controller.admin;

import com.artiv.common.Result;
import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import com.artiv.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminStatsController {
    private final AdminStatsService adminStatsService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(adminStatsService.overview());
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> operations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(adminStatsService.operations(startDate, endDate));
    }

    @GetMapping("/stats/demand")
    public Result<Map<String, Object>> demand(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(adminStatsService.demand(startDate, endDate));
    }

    @GetMapping("/stats/price-distribution")
    public Result<Map<String, Object>> priceDistribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String projectCategory, HttpServletRequest request) {
        if (request.getParameterMap().containsKey("category")) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "企划字段 category 已停用，请刷新页面后使用 projectCategory");
        }
        return Result.ok(adminStatsService.priceDistribution(startDate, endDate, projectCategory));
    }
}
