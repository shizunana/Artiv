package com.artiv.controller.admin;

import com.artiv.common.Result;
import com.artiv.dto.admin.ReviewAppealDecisionRequest;
import com.artiv.dto.admin.ReviewAppealDetail;
import com.artiv.security.UserContext;
import com.artiv.service.ReviewService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/review-appeals")
@RequiredArgsConstructor
public class AdminReviewAppealController {

    private final ReviewService reviewService;

    @GetMapping
    public Result<Page<ReviewAppealDetail>> list(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "20") long size,
                                                 @RequestParam(required = false) Integer status,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(reviewService.listAppeals(page, size, status, keyword, startDate, endDate));
    }

    @GetMapping("/{id}")
    public Result<ReviewAppealDetail> detail(@PathVariable Long id) {
        return Result.ok(reviewService.getAppeal(id));
    }

    @PutMapping("/{id}/approve")
    public Result<ReviewAppealDetail> approve(@PathVariable Long id,
                                              @Valid @RequestBody ReviewAppealDecisionRequest request) {
        return Result.ok(reviewService.resolveAppeal(id, UserContext.getUserId(), true, request.getReason()));
    }

    @PutMapping("/{id}/reject")
    public Result<ReviewAppealDetail> reject(@PathVariable Long id,
                                             @Valid @RequestBody ReviewAppealDecisionRequest request) {
        return Result.ok(reviewService.resolveAppeal(id, UserContext.getUserId(), false, request.getReason()));
    }
}
